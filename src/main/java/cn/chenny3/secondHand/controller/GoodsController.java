package cn.chenny3.secondHand.controller;

import cn.chenny3.secondHand.asyncframework.EventModel;
import cn.chenny3.secondHand.asyncframework.EventProducer;
import cn.chenny3.secondHand.asyncframework.EventType;
import cn.chenny3.secondHand.common.annotation.PermissionAnnotation;
import cn.chenny3.secondHand.common.bean.PageHelper;
import cn.chenny3.secondHand.common.bean.UserHolder;
import cn.chenny3.secondHand.common.bean.enums.GoodsStatus;
import cn.chenny3.secondHand.common.bean.dto.EasyResult;
import cn.chenny3.secondHand.common.bean.enums.RoleType;
import cn.chenny3.secondHand.common.utils.RedisUtils;
import cn.chenny3.secondHand.common.utils.RedisKeyUtils;
import cn.chenny3.secondHand.common.bean.vo.ViewObject;
import cn.chenny3.secondHand.common.utils.SecondHandUtil;
import cn.chenny3.secondHand.model.Address;
import cn.chenny3.secondHand.model.Category;
import cn.chenny3.secondHand.model.Goods;
import cn.chenny3.secondHand.model.User;
import cn.chenny3.secondHand.service.AddressService;
import cn.chenny3.secondHand.service.CategoryService;
import cn.chenny3.secondHand.service.GoodsService;
import cn.chenny3.secondHand.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping("goods")
public class GoodsController extends BaseController {
    @Autowired
    private GoodsService goodsService;
    @Autowired
    private UserService userService;
    @Autowired
    private UserHolder userHolder;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private AddressService addressService;
    @Autowired
    private RedisUtils redisUtils;
    @Autowired
    private EventProducer eventProducer;

    @PermissionAnnotation(roles = RoleType.Admin)
    @RequestMapping(value = "addView", method = RequestMethod.GET)
    public String addView(Model model) {
        return "/goods/goods_add";
    }

    @PermissionAnnotation(name="发布商品",description = "",roles = {RoleType.Admin})
    @RequestMapping(method = RequestMethod.POST)
    @ResponseBody
    public EasyResult addGoods(@Valid Goods goods, BindingResult bindingResult) {
        String action = goods.getStatus() == 1 ? "商品信息发布" : "商品信息保存";
        try {
            if (bindingResult.hasErrors()) {
                return new EasyResult(1, objectErrorsToString(bindingResult));
            }
            goods.setOwnerId(userHolder.get().getId());
            goodsService.addGoods(goods);
            return new EasyResult(0, goods.getId());
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
            return new EasyResult(1, action + "失败");
        }
    }

    @RequestMapping(value = "{id}", method = RequestMethod.GET)
    public String getDetail(@PathVariable int id, Model model) {
        try {
            if (id <= 0) {
                return "redirect:/404.html";
            }
            Goods goods = null;
            //从redis中查询缓存的商品信息
            String cache = redisUtils.get(RedisKeyUtils.getCacheGoodsKey(id));
            if (StringUtils.isNoneBlank(cache)) {
                goods= SecondHandUtil.getObjectFromJson(cache,Goods.class);
            }else{
                //如果缓存未命中，再查询数据库
                goods = goodsService.selectGoods(id);
                //并将结果缓存至reids
                redisUtils.set(RedisKeyUtils.getCacheGoodsKey(id),SecondHandUtil.getJsonString(goods));
            }

            if (goods != null) {
                //临时修改前台展示商品的访问量，将真正的更新操作交由异步线程默默处理
                goods.setViewNum(goods.getViewNum() + 1);
                //异步的更新商品访问量
                eventProducer.fireEvent(new EventModel()
                        .setEventType(EventType.VIEW)
                        .setEntityid(goods.getId())
                        .setDatas().setData("goods",goods)
                );

                //查询商品归属人
                User owner = userService.selectUser(goods.getOwnerId());
                //抹掉安全相关信息
                owner.setPassword("");
                owner.setSalt("");
                //查询商品归属人地址信息
                Address address = addressService.select(owner.getAddressId());

                ViewObject viewObject = new ViewObject().
                        put("goods", goods).
                        put("owner", owner).
                        put("address", address).
                        put("topCategory", categoryService.selectCategory(goods.getCategoryId())).
                        put("tag", categoryService.selectCategory(goods.getSubCategoryId()));

                //如果客户端处于登录状态，查询是否收藏、点赞此商品
                if (userHolder.get() != null) {
                    int userId = userHolder.get().getId();
                    viewObject.put("collectFlag", redisUtils.zrank(RedisKeyUtils.getUserCollectKey(userId), String.valueOf(id)) != null);
                    viewObject.put("hotFlag", redisUtils.zrank(RedisKeyUtils.getUserHotKey(userId), String.valueOf(id)) != null);
                }
                model.addAttribute("vo", viewObject);
                return "/goods/goods_detail";
            }
            return "redirect:/404.html";
        } catch (Exception e) {
            logger.error(e.getMessage());
            return "redirect:/404.html";
        }
    }

    @RequestMapping(value = {"/list", "/list/{categoryId}", "/list/{categoryId}/{subCategoryId}"})
    public String list(@PathVariable(required = false) Integer categoryId,
                       @PathVariable(required = false) Integer subCategoryId,
                       @RequestParam(required = false, defaultValue = "1") int curPage,
                       @RequestParam(required = false, defaultValue = "created") String orderName,
                       @RequestParam(required = false, defaultValue = "desc") String orderValue,
                       Model model) {
        if (categoryId == null || categoryId < 0) {
            categoryId = 0;
        }
        if (subCategoryId == null || subCategoryId < 0) {
            subCategoryId = 0;
        }


        if (curPage < 1) {
            curPage = 1;
        }

        int pageSize = 20;

        List<Goods> goodsList = Collections.emptyList();
        int count = 0;
        //是否按热度排序
        if ("hot".equals(orderName)) {
            count = goodsService.selectCount(categoryId, subCategoryId, GoodsStatus.PUBLISH);
            if (count > 0) {
                goodsList = goodsService.selectHotGoodsList(categoryId, subCategoryId, curPage, pageSize);
            }
        } else {
            //构造sql各部分子句
            String whereClause = " status = 1";
            if (categoryId != 0) {
                whereClause += " and category_id=" + categoryId;
                if (subCategoryId != 0) {
                    whereClause += " and sub_category_id=" + subCategoryId;
                }
            }
            String orderClause = "";
            if ("price".equals(orderName) || "created".equals(orderName)) {
                if (("desc".equalsIgnoreCase(orderValue) || "asc".equals(orderValue))) {
                    orderClause = orderName + " " + orderValue;
                }
            }

            String limitClause = (curPage - 1) * pageSize + "," + pageSize;
            count = goodsService.selectGoodsCountByClause(whereClause);
            if (count != 0) {
                goodsList = goodsService.selectGoodsListByClause(whereClause, orderClause, limitClause);
            }
        }

        PageHelper<Goods> pageHelper = new PageHelper<>();
        pageHelper.setCount(count);
        pageHelper.setCurPage(curPage);
        pageHelper.setPageSize(pageSize);
        pageHelper.setContents(goodsList);

        ViewObject conditions = new ViewObject().put("categoryId", categoryId).
                put("subCategoryId", subCategoryId).
                put("orderName", orderName).
                put("orderValue", orderValue);

        List<Category> tags = categoryId != 0 ? categoryService.selectCategoriesByParentId(categoryId) : null;

        model.addAttribute("vo", new ViewObject().put("pageHelper", pageHelper).
                put("conditions", conditions).
                put("tags", tags));
        return "/goods/goods_list";
    }

    @RequestMapping(value = "/collect/{goodsId}", method = RequestMethod.POST)
    @ResponseBody
    public EasyResult collect(@PathVariable("goodsId") int goodsId) {
        try {
            String userCollectKey = RedisKeyUtils.getUserCollectKey(userHolder.get().getId());
            String goodsCollectKey = RedisKeyUtils.getGoodsCollectKey(goodsId);
            if (redisUtils.zrank(userCollectKey, String.valueOf(goodsId)) != null) {
                return new EasyResult(1, "不能重复收藏同一件商品");
            }
            //增加收藏量
            redisUtils.incr(goodsCollectKey);
            goodsService.updateCollectNum(goodsId, 1);
            //保存当前用户的收藏记录
            redisUtils.zadd(
                    userCollectKey,
                    String.valueOf(goodsId),
                    System.currentTimeMillis()
            );
            return new EasyResult(0, "收藏成功");
        } catch (Exception e) {
            logger.error(e.getMessage());
            return new EasyResult(1, "收藏失败");
        }
    }

    @RequestMapping(value = "/collect/{goodsId}", method = RequestMethod.DELETE)
    @ResponseBody
    public EasyResult cancalCollect(@PathVariable("goodsId") int goodsId) {
        try {
            String userCollectKey = RedisKeyUtils.getUserCollectKey(userHolder.get().getId());
            String goodsCollectKey = RedisKeyUtils.getGoodsCollectKey(goodsId);
            if (redisUtils.zrank(userCollectKey, String.valueOf(goodsId)) == null) {
                return new EasyResult(1, "不能取消收藏没有收藏过的商品");
            }
            //减少收藏量
            redisUtils.decr(goodsCollectKey);
            goodsService.updateCollectNum(goodsId, -1);
            //删除当前用户的收藏记录
            redisUtils.zrem(
                    userCollectKey,
                    String.valueOf(goodsId)
            );
            return new EasyResult(0, "取消收藏成功");
        } catch (Exception e) {
            logger.error(e.getMessage());
            return new EasyResult(1, "取消收藏失败");
        }
    }

    @RequestMapping(value = "/hot/{goodsId}", method = RequestMethod.POST)
    @ResponseBody
    public EasyResult hot(@PathVariable("goodsId") int goodsId) {
        try {
            String userHotKey = RedisKeyUtils.getUserHotKey(userHolder.get().getId());
            String goodsHotKey = RedisKeyUtils.getGoodsHotKey(goodsId);
            if (redisUtils.zrank(userHotKey, String.valueOf(goodsId)) != null) {
                return new EasyResult(1, "不能重复点赞同一件商品");
            }
            //增加点赞量
            redisUtils.incr(goodsHotKey);
            goodsService.updateHotNum(goodsId, 1);
            //保存当前用户的点赞记录
            redisUtils.zadd(
                    userHotKey,
                    String.valueOf(goodsId),
                    System.currentTimeMillis()
            );
            return new EasyResult(0, "点赞成功");
        } catch (Exception e) {
            logger.error(e.getMessage());
            return new EasyResult(1, "点赞失败");
        }
    }

    @RequestMapping(value = "/hot/{goodsId}", method = RequestMethod.DELETE)
    @ResponseBody
    public EasyResult cancalHot(@PathVariable("goodsId") int goodsId) {
        try {
            String userHotKey = RedisKeyUtils.getUserHotKey(userHolder.get().getId());
            String goodsHotKey = RedisKeyUtils.getGoodsHotKey(goodsId);
            if (redisUtils.zrank(userHotKey, String.valueOf(goodsId)) == null) {
                return new EasyResult(1, "不能取消点赞没有点赞过的商品");
            }
            //增加点赞量
            redisUtils.decr(goodsHotKey);
            goodsService.updateHotNum(goodsId, -1);
            //保存当前用户的点赞记录
            redisUtils.zrem(
                    userHotKey,
                    String.valueOf(goodsId)
            );
            return new EasyResult(0, "取消点赞成功");
        } catch (Exception e) {
            logger.error(e.getMessage());
            return new EasyResult(1, "取消点赞失败");
        }
    }

    @RequestMapping(value = "/inventory/{goodsId}",method = RequestMethod.GET)
    @ResponseBody
    public EasyResult getInventory(@PathVariable int goodsId){
        try{
            int inventory=goodsService.selectInventory(goodsId);
            return new EasyResult(0,inventory);
        }catch (Exception e){
            e.printStackTrace();
            logger.error(e.getMessage());
            return new EasyResult(1,"查询商品库存出错");
        }
    }
}
