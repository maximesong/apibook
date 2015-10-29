angular.module('apibookApp', ["ui.bootstrap"])
    .config(function($locationProvider) {
        $locationProvider.html5Mode(true);
    })
    .filter('simpleName', function() {
        return function(input) {
            var fields = input.split(".");
            return fields[fields.length -1];
        }
    })
    .controller('APIBookController', ['$scope', '$http','$location', function($scope, $http, $location) {
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
            var response = $http.post("/api/search/method", searchOptions)
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
    .controller('stackoverflowController', ['$scope', '$http', '$location', '$anchorScroll', '$timeout',
        function($scope, $http, $location, $anchorScroll, $timeout) {

        $scope.showOnlyProgramTask = true;
        $scope.filterChanged = function() {
            console.log("changed", $scope.questions, $scope.showOnlyProgramTask, $scope.questionReviews)
            if ($scope.showOnlyProgramTask) {
                if ($scope.questionReviews == null) {
                    $scope.questions = []
                } else {
                     $scope.questions = _($scope.originQuestions).filter(function(question) {
                                        var review = $scope.questionReviews[question.id];
                                        return review != null && review.isProgramTask === true;
                     });
                }
                // DEBUG
                // console.log($scope.questions)
            } else {
                $scope.questions = $scope.originQuestions;
            }
            if ($scope.question) {
                for (var i = 0; i != $scope.questions.length; ++i) {
                    console.log($scope.questions.length, i)
                    if ($scope.questions[i].id == $scope.question.id) {
                        $scope.setQuestionIndex(i);
                        return;
                    }
                }
            }
            if ($scope.questions.length > 0) {
                $scope.setQuestionIndex(0);
            }
        }

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

        $scope.upsertQuestionMethodReview = function(id, canonicalName, methodFullName, relevance) {
            console.log("relevance!");
            $http.post("/api/stackoverflow/questions/" + id + "/review/method/update",
                {
                    "id": id,
                    "reviewer": "author",
                    "relevance": relevance,
                    "canonicalName": canonicalName,
                    "methodFullName": methodFullName
                }
            ).then(function(resp) {
                if (resp.status === 200) {
                    if ($scope.questionMethodReviews[id] === undefined) {
                        $scope.questionMethodReviews[id] = {};
                    }
                    $scope.questionMethodReviews[id][canonicalName] = relevance;
                    console.log($scope.questionMethodReviews);
                }
            });
        }

        $scope.onKeypress = function(event) {
            //console.log(event);
        }

        $scope.setNotProgramTask = function(id) {
            $scope.upsertQuestionReviewField(id, "isProgramTask", false);
            $scope.upsertQuestionReviewField(id, "answerIdUsingApi", 0);
            $scope.upsertQuestionReviewField(id, 'singleKeyApi', false);
        }

        $scope.setNoAnswerIdUsingApi = function(id) {
            $scope.upsertQuestionReviewField(id, 'answerIdUsingApi', 0);
            $scope.upsertQuestionReviewField(id, 'singleKeyApi', false);
        }

        $scope.setUsingMutipleApi = function(questionId, answerId) {
            $scope.upsertQuestionReviewField(questionId, "isProgramTask", true);
            $scope.upsertQuestionReviewField(questionId, 'answerIdUsingApi', answerId);
            $scope.upsertQuestionReviewField(questionId, 'singleKeyApi', false);
        }

        $scope.setUsingSingleApi = function(questionId, answerId) {
            $scope.upsertQuestionReviewField(questionId, "isProgramTask", true);
            $scope.upsertQuestionReviewField(questionId, 'answerIdUsingApi', answerId);
            $scope.upsertQuestionReviewField(questionId, 'singleKeyApi', true);
        }

        $scope.setQuestionIndex = function(i) {
            console.log("next!", i);
            $scope.questionIndex = i;
            $scope.question = $scope.questions[$scope.questionIndex];
            var searchOptions = {
                searchText: $scope.question.title
            };
            // search for title
            $scope.resultItems = []
            $timeout(function() {
                var response = $http.post("/api/search/method", searchOptions)
                response.success(function(data) {
                    $scope.resultItems = data.result;
                    console.log($scope.resultItems);
                });
            }, 0)
            //console.log($scope.question.title);
            //console.log($scope.question);
            //console.log($scope.questionReviews[$scope.question.id]);
        }

        $scope.click = function() {
            console.log("click");
        }

        $scope.jumpToAnswers = function() {
            var review = $scope.questionReviews[$scope.question.id];
            if (review.answerIdUsingApi > 0) {
                $anchorScroll("answer-" + review.answerIdUsingApi);
            } else {
                $anchorScroll("answers");
            }
        }

        $scope.jumpNextToReview = function() {
            console.log("Jump!");
            for (var i = 0; i != $scope.questions.length; ++i) {
                var question = $scope.questions[i];
                var review = $scope.questionReviews[question.id];
                if (review === undefined || review.isProgramTask == null || review.answerIdUsingApi == null ||
                    review.singleKeyApi == null) {
                    $scope.setQuestionIndex(i);
                    $anchorScroll("question");
                    return;
                }
            }
        }

        $http.get("/api/stackoverflow/questions")
            .then(function(resp) {
                $scope.originQuestions = resp.data;
                $scope.filterChanged();
            });
        $http.get("/api/stackoverflow/questions/reviews")
            .then(function(resp) {
                 $scope.questionReviews = {};
                 _(resp.data).each(function(review) {
                    $scope.questionReviews[review.id] = review
                 });
                 $scope.filterChanged();
            });
        $http.get("/api/stackoverflow/questions/method/reviews")
            .then(function(resp) {
                 $scope.questionMethodReviews = {};
                 _(resp.data).each(function(review) {
                    if ($scope.questionMethodReviews[review.questionId] == null) {
                        $scope.questionMethodReviews[review.questionId] = {}
                    }
                    $scope.questionMethodReviews[review.questionId][review.canonicalName] = review.relevance
                 });
                 //$scope.filterChanged();
            });
        console.log("Hello Stackoverflow")
    }]);

