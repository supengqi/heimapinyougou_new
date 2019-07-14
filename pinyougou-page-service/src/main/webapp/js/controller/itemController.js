app.controller('itemController',function($scope){
	
	$scope.specificationItems={}; // 存储用户选择的规格
	
	// 购物车数量加减
	$scope.addNum=function(x) {
		$scope.num+=x;
		if($scope.num < 1) {
			$scope.num = 1;
		}
	}
	
	// 用户选择规格
	$scope.selectSpecification=function(key,value) {
		specificationItems[key]=value;
	}
});