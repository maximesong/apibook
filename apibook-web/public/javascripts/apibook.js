angular.module('apibookApp', [])
    .config(function($locationProvider) {
        $locationProvider.html5Mode(true);
    })
    .controller('APIBookController', ['$scope', '$http', function($scope, $http, $location) {
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
    .controller('stackoverflowController', ['$scope', '$http', '$location', '$anchorScroll', function($scope, $http, $location, $anchorScroll) {
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
                    if ($scope.questionReviews[id] === undefined) {
                        $scope.questionReviews[id] = {};
                    }
                    $scope.questionReviews[id][field] = value;
                }
            });
        }

        $scope.setNotProgramTask = function(id) {
            $scope.upsertQuestionReviewField(id, "isProgramTask", false);
            $scope.upsertQuestionReviewField(id, "answerIdUsingApi", 0);
            $scope.upsertQuestionReviewField(id, 'singleKeyApi', false);
        }

        $scope.setUsingMutipleApi = function(questionId, answerId) {
            $scope.upsertQuestionReviewField(id, "isProgramTask", true);
            $scope.upsertQuestionReviewField(questionId, 'answerIdUsingApi', answerId);
            $scope.upsertQuestionReviewField(questionId, 'singleKeyApi', false);
        }

        $scope.setUsingSingleApi = function(questionId, answerId) {
            $scope.upsertQuestionReviewField(id, "isProgramTask", true);
            $scope.upsertQuestionReviewField(questionId, 'answerIdUsingApi', answerId);
            $scope.upsertQuestionReviewField(questionId, 'singleKeyApi', true);
        }

        $scope.setQuestionIndex = function(i) {
            console.log("next!", i);
            $scope.questionIndex = i;
            $scope.question = $scope.questions[$scope.questionIndex];
            console.log($scope.question);
            console.log($scope.questionReviews[$scope.question.id]);
        }

        $scope.click = function() {
            console.log("click");
        }

        $scope.jumpToAnswers = function() {
            $anchorScroll("answers");
        }

        $scope.jumpNextToReview = function() {
            console.log("Jump!");
            for (var i = 0; i != $scope.questions.length; ++i) {
                var question = $scope.questions[i];
                review = $scope.questionReviews[question.id];
                if (review === undefined || review.isProgramTask == null || review.answerIdUsingApi == null ||
                    review.singleKeyApi == null) {
                    $scope.setQuestionIndex(i);
                    return;
                }
            }
        }

        $http.get("/api/stackoverflow/questions")
            .then(function(resp) {
                $scope.questions = resp.data;
                $scope.setQuestionIndex(0);
            });
        $http.get("/api/stackoverflow/questions/reviews")
            .then(function(resp) {
             $scope.questionReviews = {};
             _(resp.data).each(function(review) {
                $scope.questionReviews[review.id] = review
             });
            });
        console.log("Hello Stackoverflow")
    }]);

