(ns jmshelby.lot-o-odds.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [jmshelby.lot-o-odds.core :as core]))

(deftest test-greet
  (testing "Greet returns correct greeting"
    (is (= "Hello, World!" (core/greet "World")))
    (is (= "Hello, Clojure!" (core/greet "Clojure")))))
