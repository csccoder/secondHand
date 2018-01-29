package cn.chenny3.secondHand.controller.web;

import cn.chenny3.secondHand.common.bean.UserHolder;
import cn.chenny3.secondHand.common.bean.dto.EasyResult;
import cn.chenny3.secondHand.common.utils.HnistPortalUtil;
import cn.chenny3.secondHand.controller.BaseController;
import cn.chenny3.secondHand.model.User;
import cn.chenny3.secondHand.model.UserAuthenticate;
import cn.chenny3.secondHand.service.UserAuthenticateService;
import cn.chenny3.secondHand.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class UserAuthenticateController extends BaseController{
    @Autowired
    private UserAuthenticateService userAuthenticateService;
    @Autowired
    private HnistPortalUtil hnistPortalUtil;
    @Autowired
    private UserService userService;
    @Autowired
    private UserHolder userHolder;

    @RequestMapping(value = "member/authenticate",method = RequestMethod.POST)
    @ResponseBody
    public EasyResult authenticate(@RequestParam("jsessionId")String hnistJsessionId){
        try {
            User user= userHolder.get();
            if (user.getAuthenticateId()>0) {
                return new EasyResult(1,"用户已认证，无需操作");
            }
            //通过OKHTTP 获取用户身份信息
            UserAuthenticate authenticateInfo = hnistPortalUtil.getAuthenticateInfo(hnistJsessionId);
            //保存至数据库
            userAuthenticateService.addAuthenticate(authenticateInfo);
            //修改当前登录用户的认证状态
            user.setAuthenticateId(authenticateInfo.getId());
            //将授权状态同步至数据库
            userService.updateAuthenticateStatus(user);
            return new EasyResult(0,"认证成功");
        } catch (Exception e) {
            logger.error(e.getMessage());
            return new EasyResult(1,"认证失败,请重新按步骤获取jessionId");
        }
    }
}
