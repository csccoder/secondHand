package cn.chenny3.secondHand.service.impl;


import cn.chenny3.secondHand.dao.CartDao;
import cn.chenny3.secondHand.model.Cart;
import cn.chenny3.secondHand.service.CartService;
import cn.chenny3.secondHand.service.GoodsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class CartServiceImpl implements CartService {
    @Autowired
    GoodsService goodsService;
    @Autowired
    CartDao cartDao;

    @Override
    public void addCart(Cart cart) {
       //检测用户购物车是否已经存在指定商品记录
        Cart cartAtDB = cartDao.selectSpecialGoodsAtCart(cart.getGoodsId(), cart.getUserId());
        if(cartAtDB==null){//不存在，添加一条新纪录至购物车表
            cart.setStatus(1);
            cart.setCreated(new Date());
            cart.setUpdated(cart.getCreated());
            cartDao.addCart(cart);
        }else{//存在，修改购物车中商品的数量
           if(cartAtDB.getStatus()==0){//该商品记录之前在购物车中被删除
               //修改状态
               cartDao.updateStatus(cartAtDB.getGoodsId(),cartAtDB.getUserId(),1);
               cartAtDB.setNum(cart.getNum());
               cartAtDB.setUpdated(new Date());
               cartDao.updateCart(cartAtDB);
           }else{
               cartAtDB.setUpdated(new Date());
               int newNum=cartAtDB.getNum()+cart.getNum();
               cartAtDB.setNum(newNum);
               cartDao.updateCart(cartAtDB);
           }
        }
    }

    @Override
    public void updateCart(Cart cart) {
        cart.setUpdated(new Date());
        cartDao.updateCart(cart);
    }

    @Override
    public void deleteCart(int goodsId,int userId) {
        cartDao.updateStatus(goodsId,userId,0);
    }

    @Override
    public List<Cart> selectCartList(int userId, int curPage, int pageSize) {
        return cartDao.selectCartList(userId, (curPage - 1) * pageSize, pageSize);
    }

    @Override
    public List<Cart> selectCartList(int userId) {
        return cartDao.selectAllCart(userId);
    }

    @Override
    public int selectCartCount(int userId) {
        return cartDao.selectCartCount(userId);
    }

    @Override
    public Cart selectCart(int userId, int goodsId) {
        return cartDao.selectSpecialGoodsAtCart(goodsId,userId);
    }


}
