angular.module('apibookApp', [])
    .controller('APIBookController', ['$scope', '$http', function($scope, $http) {
        console.log("HI");
        var postJson = function (url, data, success, error) {
            $.ajax({
                method: "POST",
                contentType: "application/json",
                url: url,
                data: data,
                success: success,
                error: error,
                dataType: "json"
            });
        };
        $scope.search = function () {
            var searchOptions = {
                searchText: $scope.searchText
            };
            var response = $http.post("/api/search", searchOptions)
            response.success(function(data) {
                console.log(data);
                $scope.items = data.result;
                console.log($scope.items);
            })
            /*
            console.log("Search");
            postJson("/api/search", JSON.stringify(searchOptions), function (data) {
                console.log(data);
                $scope.items = data.result;
                $digest();
            });
            */
        };
        $scope.keypress = function(event) {
            if (event.keyCode === 13) {
                $scope.search();
            }
        }
    }])
    .controller('stackoverflowController', ['$scope', '$http', function($scope, $http) {
        $scope.upsertQuestionReviewField = function(id, field, value) {
            console.log("update!");
            $http.post("/api/stackoverflow/questions/" + id + "/review/update",
                {
                    "id": id,
                    "reviewer": "author",
                    "field": field,
                    "value": value
                }
            ).then(function(resp) {
                if (resp.status === 200) {
                    $scope.questionReviews[id][field] = value;
                }
            });
        }

        $http.get("/api/stackoverflow/questions")
            .then(function(resp) {
                $scope.questions = resp.data
                console.log(resp.data)
            });
        $http.get("/api/stackoverflow/questions/reviews")
            .then(function(resp) {
             $scope.questionReviews = {};
             _(resp.data).each(function(review) {
                $scope.questionReviews[review.id] = review
             });
             console.log($scope.questionReviews);
            });
        console.log("Hello Stackoverflow")
    }]);

