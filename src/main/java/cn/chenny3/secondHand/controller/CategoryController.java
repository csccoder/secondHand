package cn.chenny3.secondHand.controller;

import cn.chenny3.secondHand.common.bean.dto.EasyResult;
import cn.chenny3.secondHand.model.Category;
import cn.chenny3.secondHand.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.validation.Valid;
import java.util.List;

@Controller
@RequestMapping("/category")
public class CategoryController extends BaseController {
    @Autowired
    CategoryService categoryService;



    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    public EasyResult get(@RequestParam int categoryId){
        try{
            if(categoryId<0){
                return new EasyResult(1, "参数取值错误");
            }
            List<Category> categories = categoryService.selectCategoriesByParentId(categoryId);
            if(categories==null||categories.size()==0){
                return new EasyResult(1, "该类目信息为空");
            }
            return new EasyResult(0, categories);
        }catch (Exception e){
            logger.error(e.getMessage());
            return new EasyResult(1, "类目信息获取失败");
        }

    }
}
