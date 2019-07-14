package com.pinyougou.content.service.impl;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.pinyougou.content.service.ContentService;
import com.pinyougou.mapper.TbContentMapper;
import com.pinyougou.pojo.TbContent;
import com.pinyougou.pojo.TbContentExample;
import com.pinyougou.pojo.TbContentExample.Criteria;

import entity.PageResult;

/**
 * 服务实现层
 * @author Administrator
 *
 */
@Service
public class ContentServiceImpl implements ContentService {

	@Autowired
	private TbContentMapper contentMapper;
	
	/**
	 * 查询全部
	 */
	@Override
	public List<TbContent> findAll() {
		return contentMapper.selectByExample(null);
	}

	/**
	 * 按分页查询
	 */
	@Override
	public PageResult findPage(int pageNum, int pageSize) {
		PageHelper.startPage(pageNum, pageSize);		
		Page<TbContent> page=   (Page<TbContent>) contentMapper.selectByExample(null);
		return new PageResult(page.getTotal(), page.getResult());
	}

	/**
	 * 增加
	 */
	@Override
	public void add(TbContent content) {
		contentMapper.insert(content);	
		// 清楚缓存
		redisTemplate.boundHashOps("content").delete(content.getCategoryId());
	}

	
	/**
	 * 修改
	 */
	@Override
	public void update(TbContent content){
		// 查询原来的分组ID
		Long categoryId = contentMapper.selectByPrimaryKey(content.getId()).getCategoryId();
		// 清楚原来缓存
		redisTemplate.boundHashOps("content").delete(categoryId);
		contentMapper.updateByPrimaryKey(content);
		// 清楚缓存
		if(categoryId.longValue() != content.getCategoryId().longValue()) { // 原来ID和修改后ID不一样后才清楚修改后缓存 性能变快
			redisTemplate.boundHashOps("content").delete(content.getCategoryId());
		}
		
	}	
	
	/**
	 * 根据ID获取实体
	 * @param id
	 * @return
	 */
	@Override
	public TbContent findOne(Long id){
		return contentMapper.selectByPrimaryKey(id);
	}

	/**
	 * 批量删除
	 */
	@Override
	public void delete(Long[] ids) {
		for(Long id:ids){
			// 清除缓存 在删除之前
			Long categoryId = contentMapper.selectByPrimaryKey(id).getCategoryId();
			redisTemplate.boundHashOps("content").delete(categoryId);
			
			contentMapper.deleteByPrimaryKey(id);
		}		
	}
	
	
		@Override
	public PageResult findPage(TbContent content, int pageNum, int pageSize) {
		PageHelper.startPage(pageNum, pageSize);
		
		TbContentExample example=new TbContentExample();
		Criteria criteria = example.createCriteria();
		
		if(content!=null){			
						if(content.getTitle()!=null && content.getTitle().length()>0){
				criteria.andTitleLike("%"+content.getTitle()+"%");
			}
			if(content.getUrl()!=null && content.getUrl().length()>0){
				criteria.andUrlLike("%"+content.getUrl()+"%");
			}
			if(content.getPic()!=null && content.getPic().length()>0){
				criteria.andPicLike("%"+content.getPic()+"%");
			}
			if(content.getStatus()!=null && content.getStatus().length()>0){
				criteria.andStatusLike("%"+content.getStatus()+"%");
			}
	
		}
		
		Page<TbContent> page= (Page<TbContent>)contentMapper.selectByExample(example);		
		return new PageResult(page.getTotal(), page.getResult());
	}
		
		@Autowired
		private RedisTemplate redisTemplate;
		
		@Override
		public List<TbContent> findByCategoryId(Long categoryId) {
			// 先查缓存里有没有
			List<TbContent> list = (List<TbContent>)redisTemplate.boundHashOps("content").get(categoryId);
			// 缓存没有 查数据库 再添加到缓存中
			if(list == null) {
				System.out.println("从数据库中查询数据并放入缓存");
				// 根据广告分类ID查询广告列表
				TbContentExample example = new TbContentExample();
				Criteria criteria = example.createCriteria();
				criteria.andCategoryIdEqualTo(categoryId); // 指定条件 分类ID
				criteria.andStatusEqualTo("1"); // 指定条件：有效
				example.setOrderByClause("sort_order"); // 排序 数据库字段
				list = contentMapper.selectByExample(example);
				// 添加到缓存中
				redisTemplate.boundHashOps("content").put(categoryId, list); // 
			} else {
				System.out.println("从缓存中查询数据");
			}
			
			return list;
		}
	
}
