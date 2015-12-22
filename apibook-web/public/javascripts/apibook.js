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
    .controller('APIBookController', ['$scope', '$http','$location', '$timeout', function($scope, $http, $location, $timeout) {
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

        $scope.findCodeSnippets = function(canonicalName, item) {
            $http.post("/api/search/snippets", {
                "canonicalName": canonicalName
            }).then(function (resp) {
                if (resp.status == 200) {
                    item.codeSnippets = resp.data.result
                    $timeout(function() {
                        $('pre code').each(function(i, block) {
                           hljs.highlightBlock(block);
                        });
                    }, 0);
                    console.log(item)
                }
                console.log(resp.data)
            });
        }
        $scope.keypress = function(event) {
            if (event.keyCode === 13) {
                $scope.search();
            }
        }
    }])
    .controller('experimentController', ['$scope', '$http', '$uibModal', function($scope, $http, $uibModal) {
        $scope.searchEngines = ["V0", "V1", "V2", "GodMode"]

        $scope.searchEngine = "V2"
        $scope.openNewQuestionModal = function() {
            var modalInstance = $uibModal.open({
                templateUrl: "assets/templates/newExperimentModal.html"
            });
            modalInstance.result.then(function(question) {
                            console.log(question);
                var newQuestion = {
                   question: question.question,
                   stackOverflowQuestionId: question.stackOverflowQuestionId,
                   reviews: [],
                   tags: []
               }
               $http.post("/api/stackoverflow/experiment/question", newQuestion);
               $scope.questions.push(newQuestion);
            })
        }
         $scope.openEditQuestionModal = function(question) {
                    var modalInstance = $uibModal.open({
                        templateUrl: "assets/templates/editExperimentModal.html",
                        resolve: {
                            question: function() {
                                return question;
                            }
                        },
                        controller: function ($scope, question) {
                         console.log("question");
                         $scope.question = angular.copy(question);
                         $scope.question.shortNameTypes = question.shortNameTypes.join(',');
                         $scope.question.longNameTypes = question.longNameTypes.join(',');
                         $scope.question.primitiveTypes = question.primitiveTypes.join(',');
                         $scope.question.implicitTypes = question.implicitTypes.join(',');
                         $scope.question.arrayTypes = question.arrayTypes.join(',');
                        }
                    });

                    modalInstance.result.then(function(updated) {
                        console.log("updated")
                        console.log(updated)
                        var removeEmpty = function(value) {
                            return value !== ""
                        }
                        question.stackOverflowQuestionId = updated.stackOverflowQuestionId;
                        question.question = updated.question;
                        question.shortNameTypes = updated.shortNameTypes.split(',').filter(removeEmpty);
                        question.longNameTypes = updated.longNameTypes.split(',').filter(removeEmpty);
                        question.primitiveTypes = updated.primitiveTypes.split(',').filter(removeEmpty);
                        question.implicitTypes = updated.implicitTypes.split(',').filter(removeEmpty);
                        question.arrayTypes = updated.arrayTypes.split(',').filter(removeEmpty);
                        console.log("question")
                        console.log(question);
                        $http.post("/api/stackoverflow/experiment/question", question);
                        //$scope.questions.push(newQuestion);
                    })
                }

        $scope.experimentOnQuestion = function(question) {
            $scope.question = question
            $scope.methodScores = []
            question.relevanceRank = null;
            var types = question.shortNameTypes.concat(question.longNameTypes).concat(
                question.implicitTypes).concat(question.primitiveTypes).concat(
                    question.arrayTypes.filter(function(t) {return t !== "array"}))
             console.log(types);
             console.log(question.arrayTypes.filter(function(t) {return t !== "array"}))
            $http.post("/api/search/method", {
                searchText: $scope.question.question,
                searchEngine: $scope.searchEngine,
                godModeTypes: types
            }).then(function(res) {
                console.log(res)
                $scope.methodScores = res.data.result;
                var relevanceRank = {}
                var rank = 1
                _($scope.methodScores).each(function(methodScore) {
                    _(question.reviews).each(function(review) {
                        if (review.canonicalName ===  methodScore.codeMethod.canonicalName) {
                            if (relevanceRank[review.relevance] == null) {
                                relevanceRank[review.relevance] = rank
                            }
                        }
                    });
                    rank += 1;
                })
                question.relevanceRank = relevanceRank
            })
        }

        $scope.removeQuestion = function(question) {
            console.log("WHY?")
            $http.post("/api/stackoverflow/experiment/question/remove", {
                question: question.question,
            }).then(function(res) {
                if (res.status === 200) {
                    $scope.questions = _($scope.questions).filter(function(q) {
                        return q.question !== question.question
                    })
                }
            })
        }

        $scope.updateReview = function(question, canonicalName, relevance) {
            console.log($scope.question);
            if (question.reviews == null) {
                question.reviews = {}
            }
            var exists = false;
            _(question.reviews).each(function(review) {
                if (review.canonicalName === canonicalName) {
                    review.relevance = relevance
                    exists = true
                }
            })
            if (!exists) {
                question.reviews.push({
                    canonicalName: canonicalName,
                    relevance: relevance
                })
            }
            $http.post("/api/stackoverflow/experiment/question", question)
        }

        $scope.getRelevance = function(question, canonicalName) {
            for (var i = 0; i != question.reviews.length; ++i) {
                review = question.reviews[i];
                if (review.canonicalName === canonicalName) {
                    return review.relevance;
               }
            }
            return null;
        }

        $http.get("/api/stackoverflow/experiment/questions")
            .then(function(res) {
            console.log(res)
            $scope.questions = res.data
        });

        console.log("Happy Experiment!")
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

