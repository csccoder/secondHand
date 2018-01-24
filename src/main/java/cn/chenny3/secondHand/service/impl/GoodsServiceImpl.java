package cn.chenny3.secondHand.service.impl;

import cn.chenny3.secondHand.dao.GoodsDao;
import cn.chenny3.secondHand.model.Goods;
import cn.chenny3.secondHand.service.GoodsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class GoodsServiceImpl implements GoodsService{
    @Autowired
    private GoodsDao goodsDao;

    @Override
    public int addGoods(Goods goods) {
        goods.setCreated(new Date());
        goods.setUpdated(goods.getCreated());
        return goodsDao.addGoods(goods);
    }

    @Override
    public Goods selectGoods(int id) {
        return goodsDao.selectGoods(id);
    }

    @Override
    public int selectCount(int status) {
        return goodsDao.selectCount(status);
    }

    @Override
    public int updateStatus(int id, int status) {
        return goodsDao.updateStatus(id,status);
    }

    @Override
    public int updateViewNum(int id, int viewNum) {
        return goodsDao.updateViewNum(id, viewNum);
    }

    @Override
    public int updateCollectNum(int id, int collectNum) {
        return goodsDao.updateCollectNum(id, collectNum);
    }

    @Override
    public int updateHotNum(int id, int hotNum) {
        return goodsDao.updateHotNum(id, hotNum);
    }

    @Override
    public List<Goods> selectGoodsBySubCategoryId(int subCategoryId) {
        return goodsDao.selectGoodsBySubCategoryId(subCategoryId);
    }

    @Override
    public List<Goods> selectGoodsByCategoryId(int categoryId) {
        return goodsDao.selectGoodsBySubCategoryId(categoryId);
    }

    @Override
    public List<Goods> selectHotGoodsList(int categoryId, int maxSize) {
        return null;
    }
}
