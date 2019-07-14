package com.pinyougou.search.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.FilterQuery;
import org.springframework.data.solr.core.query.GroupOptions;
import org.springframework.data.solr.core.query.HighlightOptions;
import org.springframework.data.solr.core.query.HighlightQuery;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.SimpleFilterQuery;
import org.springframework.data.solr.core.query.SimpleHighlightQuery;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.data.solr.core.query.result.GroupEntry;
import org.springframework.data.solr.core.query.result.GroupPage;
import org.springframework.data.solr.core.query.result.GroupResult;
import org.springframework.data.solr.core.query.result.HighlightEntry;
import org.springframework.data.solr.core.query.result.HighlightEntry.Highlight;
import org.springframework.data.solr.core.query.result.HighlightPage;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.dubbo.config.annotation.Service;
import com.pinyougou.pojo.TbItem;
import com.pinyougou.search.service.ItemSearchService;   

@Service(timeout=5000)
public class ItemSearchServiceImpl implements ItemSearchService{
	
	@Autowired
	private SolrTemplate solrTemplate;
	
	@Override
	public Map<String, Object> search(Map searchMap) {
		Map map  = new HashMap();
		
		/*Query query = new SimpleQuery("*:*");
		// 添加查询条件
		Criteria criteria = new Criteria("item_keywords").is(searchMap.get("keywords" ));
		query.addCriteria(criteria);
		ScoredPage<TbItem> page = solrTemplate.queryForPage(query, TbItem.class);
		map.put("rows", page.getContent());*/
		
		//关键字空格处理
		String keywords = (String) searchMap.get("keywords");
		System.out.println(keywords);
		searchMap.put("keywords", keywords.replace(" ", ""));
		
		// 1.查询列表 
		map.putAll(searchList(searchMap));
		
		// 2.分组查询 商品分类列表
		List<String> categoryList = searchCategoryList(searchMap);
		map.put("categoryList", categoryList);
		
		// 3. 查询品牌和规格列表
		String category = (String) searchMap.get("category");
		if (!"".equals(category)) { //如果有分类名称
			map.putAll(searchBrandAndSpecList(category));
		} else { //如果没有分类名称，按照第一个查询
			if (categoryList.size() > 0) {
				map.putAll(searchBrandAndSpecList(categoryList.get(0)));
			}
		}

		return map;
	}

	
	private Map searchList(Map searchMap) {
		Map map  = new HashMap();
		
		// 高亮选项初始化
		HighlightQuery  query = new SimpleHighlightQuery();
		HighlightOptions highlightOptions = new HighlightOptions().addField("item_title"); // 设置高亮的域
		highlightOptions.setSimplePrefix("<em style='color:red'>"); // 高亮前缀
		highlightOptions.setSimplePostfix("</em>");  // 高亮后缀
		
		query.setHighlightOptions(highlightOptions); // 设置高亮选项
		
		// 1.1关键字查询
		Criteria criteria = new Criteria("item_keywords").is(searchMap.get("keywords" ));
		query.addCriteria(criteria);
		
		//1.2 按商品分类过滤
		if(!"".equals(searchMap.get("category"))  )	{//如果用户选择了分类 
			FilterQuery filterQuery=new SimpleFilterQuery();
			Criteria filterCriteria=new Criteria("item_category").is(searchMap.get("category"));
			filterQuery.addCriteria(filterCriteria);
			query.addFilterQuery(filterQuery);			
		}
		
		//1.3 按品牌过滤
		if(!"".equals(searchMap.get("brand"))  )	{//如果用户选择了品牌
			FilterQuery filterQuery=new SimpleFilterQuery();
			Criteria filterCriteria=new Criteria("item_brand").is(searchMap.get("brand"));
			filterQuery.addCriteria(filterCriteria);
			query.addFilterQuery(filterQuery);			
		}
		
		//1.4 按规格过滤
		if(searchMap.get("spec")!=null){			
			Map<String,String> specMap= (Map<String, String>) searchMap.get("spec");
			for(String key :specMap.keySet()){	
				FilterQuery filterQuery=new SimpleFilterQuery();
				Criteria filterCriteria=new Criteria("item_spec_"+key).is( specMap.get(key)  );
				filterQuery.addCriteria(filterCriteria);
				query.addFilterQuery(filterQuery);						
			}		
					
		}
		
		// 1.5 按价格过滤
		if(!"".equals(searchMap.get("price"))  )	{//如果用户选择了价格
			String priceStr = (String) searchMap.get("price");
			String[] price = priceStr.split("-");
			// <=500
			// 2000-3000
			// * >= 3000
			if(!"0".equals(price[0])) {    // 如果价格不等于0
				FilterQuery filterQuery=new SimpleFilterQuery();
				Criteria filterCriteria=new Criteria("item_price").greaterThanEqual(price[0]);
				filterQuery.addCriteria(filterCriteria);
				query.addFilterQuery(filterQuery);	
			}
			
			if(!"*".equals(price[1])) {    // 如果价格不等于*  最大
				FilterQuery filterQuery=new SimpleFilterQuery();
				Criteria filterCriteria=new Criteria("item_price").lessThan(price[1]);
				filterQuery.addCriteria(filterCriteria);
				query.addFilterQuery(filterQuery);	
			}
					
		}
		
		// ********分页**************
		
		//1.6 分页
		Integer pageNo= (Integer) searchMap.get("pageNo");//获取页码
		if(pageNo==null){
			pageNo=1;
		}
		Integer pageSize= (Integer) searchMap.get("pageSize");//获取页大小
		if(pageSize==null){
			pageSize=20;
		}
				
		query.setOffset( (pageNo-1)*pageSize  );//起始索引
		query.setRows(pageSize);//每页记录数
		
		// 价格排序
		String sortValue = (String) searchMap.get("sort"); // 升序ASC 降序DESC
		String sortField = (String) searchMap.get("sortField"); // 升序ASC 降序DESC
		
		if (sortValue != null && !"".equals(sortValue)) {
			if ("ASC".equals(sortValue)) {
				Sort sort = new Sort(Sort.Direction.ASC, "item_" + sortField); // 排序字段升序
				query.addSort(sort);
			}
			if ("DESC".equals(sortValue)) {
				Sort sort = new Sort(Sort.Direction.DESC, "item_" + sortField); // 排序字段升序
				query.addSort(sort);
			}
		}
		
		
		Sort sort = new Sort(Sort.Direction.ASC, "item_price"); // 价格升序
		query.addSort(sort);
		
		
		
		
		// ********获取高亮对象结果集*****
		
		// 高亮页的对象
		HighlightPage<TbItem> page = solrTemplate.queryForHighlightPage(query, TbItem.class);
		
		// 高亮入口集合
		List<HighlightEntry<TbItem>> entryList = page.getHighlighted();
		for(HighlightEntry<TbItem> entry : entryList) {
			// 获取高亮列表(高亮域的个数)
			List<Highlight> highlightList = entry.getHighlights();
			
			/*for(Highlight h : highlightList) {
				List<String> sns = h.getSnipplets(); // 每个域有可能存在多个值
				System.out.println(sns);
			}*/
			
			if(highlightList.size() > 0 && highlightList.get(0).getSnipplets().size() > 0) {
				TbItem item = entry.getEntity();
				String title = highlightList.get(0).getSnipplets().get(0);
				item.setTitle(title);
			}
			
			
		}
		map.put("rows",page.getContent());
		map.put("totalPages", page.getTotalPages());//返回总页数
		map.put("total", page.getTotalElements());//返回总记录数
		return map;
	}
	
	private void get(String string) {
		// TODO Auto-generated method stub
		
	}


	/**
	 * 分组查询 商品分类列表
	 * @param searchMap
	 * @return
	 */
	private List searchCategoryList(Map searchMap) {
		List<String> list=new ArrayList();
		
		Query query = new SimpleQuery("*:*");
		// 关键字查询
		Criteria criteria = new Criteria("item_keywords").is(searchMap.get("keywords" )); 
		query.addCriteria(criteria);
		//设置分组选项
		GroupOptions groupOptions = new GroupOptions().addGroupByField("item_category"); // 相当于 group by 可以加多个分组 
		query.setGroupOptions(groupOptions);
		
		// 获取分组页对象
		GroupPage<TbItem> page = solrTemplate.queryForGroupPage(query, TbItem.class);
		// 获取分组结果对象
		GroupResult<TbItem> groupResult = page.getGroupResult("item_category");
		
		// 获取分组入口页
		Page<GroupEntry<TbItem>> groupEntries = groupResult.getGroupEntries();
		// 获取分组入口集合
		List<GroupEntry<TbItem>> entryList = groupEntries.getContent();
		for (GroupEntry<TbItem> entry : entryList) {
			list.add(entry.getGroupValue()); //将分组结果的名称封装到返回值中
	
		}
		return list;
	}
	
	@Autowired
	private RedisTemplate redisTemplate;
	/**
	 * 查询品牌和规格列表
	 * @param searchMap
	 * @return
	 */
	private Map searchBrandAndSpecList(String category) {
		
		Map map = new HashMap();
		// 1.根据商品分类名称得到模板ID
		Long templateId = (Long)redisTemplate.boundHashOps("itemCat").get(category);
		
		if(templateId != null) {
			// 2.根据模板ID 获取品牌列表
			List brandList = (List)redisTemplate.boundHashOps("brandList").get(templateId);
			map.put("brandList", brandList);
			// 3.根据模板ID获取规格列表
			List specList = (List)redisTemplate.boundHashOps("specList").get(templateId);
			map.put("specList", specList);
		}
		
		return map;
	
	}

	
	/**
	 * 导入数据
	 */
	@Override
	public void importList(List list) {
		solrTemplate.saveBeans(list);
		solrTemplate.commit();
	}

	/**
	 * 删除商品
	 */
	@Override
	public void deleteByGoodsIds(List goodsIds) {
		System.out.println("删除商品ID" + goodsIds);
		Query query  = new SimpleQuery("*:*");
		Criteria criteria = new Criteria("item_goodsid").in(goodsIds);
		query.addCriteria(criteria);
		// 删除
		solrTemplate.delete(query);
		solrTemplate.commit();
	}
}
