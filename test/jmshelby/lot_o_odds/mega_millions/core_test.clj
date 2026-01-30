(ns jmshelby.lot-o-odds.mega-millions.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [jmshelby.lot-o-odds.mega-millions.core :as mm]))

;; Test helper data
(def test-numbers [5 12 23 34 45])
(def test-mega-ball 7)
(def test-jackpot 100000000)

;; ============================================================================
;; Validation Tests
;; ============================================================================

(deftest test-valid-main-number
  (testing "Valid main numbers (1-70)"
    (is (true? (mm/valid-main-number? 1)))
    (is (true? (mm/valid-main-number? 35)))
    (is (true? (mm/valid-main-number? 70))))

  (testing "Invalid main numbers"
    (is (false? (mm/valid-main-number? 0)))
    (is (false? (mm/valid-main-number? 71)))
    (is (false? (mm/valid-main-number? -5)))
    (is (false? (mm/valid-main-number? 1.5)))))

(deftest test-valid-mega-ball
  (testing "Valid mega ball numbers (1-24)"
    (is (true? (mm/valid-mega-ball? 1)))
    (is (true? (mm/valid-mega-ball? 12)))
    (is (true? (mm/valid-mega-ball? 24))))

  (testing "Invalid mega ball numbers"
    (is (false? (mm/valid-mega-ball? 0)))
    (is (false? (mm/valid-mega-ball? 25)))
    (is (false? (mm/valid-mega-ball? -1)))
    (is (false? (mm/valid-mega-ball? 5.5)))))

;; ============================================================================
;; Ticket and Drawing Tests
;; ============================================================================

(deftest test-create-ticket
  (testing "Create valid ticket with multiplier"
    (let [ticket (mm/create-ticket test-numbers test-mega-ball 2)]
      (is (= #{5 12 23 34 45} (:main-numbers ticket)))
      (is (= 7 (:mega-ball ticket)))
      (is (= 2 (:multiplier ticket)))))

  (testing "Ticket gets random multiplier if not specified"
    (let [ticket (mm/create-ticket test-numbers test-mega-ball)]
      (is (contains? #{2 3 4 5 10} (:multiplier ticket)))))

  (testing "Invalid ticket - wrong count"
    (is (thrown? AssertionError (mm/create-ticket [1 2 3] 5 2))))

  (testing "Invalid ticket - duplicate numbers"
    (is (thrown? AssertionError (mm/create-ticket [1 2 3 4 4] 5 2))))

  (testing "Invalid ticket - invalid multiplier"
    (is (thrown? AssertionError (mm/create-ticket test-numbers test-mega-ball 6)))))

(deftest test-create-drawing
  (testing "Create valid drawing"
    (let [drawing (mm/create-drawing test-numbers test-mega-ball test-jackpot)]
      (is (= #{5 12 23 34 45} (:main-numbers drawing)))
      (is (= 7 (:mega-ball drawing)))
      (is (= test-jackpot (:jackpot drawing))))))

;; ============================================================================
;; Random Generation Tests
;; ============================================================================

(deftest test-generate-random-main-numbers
  (testing "Generates 5 numbers"
    (let [numbers (mm/generate-random-main-numbers)]
      (is (= 5 (count numbers)))))

  (testing "All numbers are unique"
    (let [numbers (mm/generate-random-main-numbers)]
      (is (= 5 (count (distinct numbers))))))

  (testing "All numbers are in valid range (1-70)"
    (let [numbers (mm/generate-random-main-numbers)]
      (is (every? #(and (>= % 1) (<= % 70)) numbers)))))

(deftest test-generate-random-mega-ball
  (testing "Mega ball is in valid range (1-24)"
    (dotimes [_ 20]
      (let [mb (mm/generate-random-mega-ball)]
        (is (>= mb 1))
        (is (<= mb 24))))))

(deftest test-generate-random-multiplier
  (testing "Multiplier is valid"
    (dotimes [_ 20]
      (let [mult (mm/generate-random-multiplier)]
        (is (contains? #{2 3 4 5 10} mult))))))

(deftest test-generate-random-ticket
  (testing "Generates valid ticket"
    (let [ticket (mm/generate-random-ticket)]
      (is (= 5 (count (:main-numbers ticket))))
      (is (mm/valid-mega-ball? (:mega-ball ticket)))
      (is (contains? #{2 3 4 5 10} (:multiplier ticket))))))

;; ============================================================================
;; Matching Tests
;; ============================================================================

(deftest test-count-matches
  (testing "Perfect match - all 5 + mega ball"
    (let [ticket (mm/create-ticket [1 2 3 4 5] 10 2)
          drawing (mm/create-drawing [1 2 3 4 5] 10 test-jackpot)
          matches (mm/count-matches ticket drawing)]
      (is (= 5 (:main-matches matches)))
      (is (= 1 (:mega-ball-match matches)))))

  (testing "5 main numbers, no mega ball"
    (let [ticket (mm/create-ticket [1 2 3 4 5] 10 2)
          drawing (mm/create-drawing [1 2 3 4 5] 11 test-jackpot)
          matches (mm/count-matches ticket drawing)]
      (is (= 5 (:main-matches matches)))
      (is (= 0 (:mega-ball-match matches)))))

  (testing "No matches"
    (let [ticket (mm/create-ticket [1 2 3 4 5] 10 2)
          drawing (mm/create-drawing [60 61 62 63 64] 11 test-jackpot)
          matches (mm/count-matches ticket drawing)]
      (is (= 0 (:main-matches matches)))
      (is (= 0 (:mega-ball-match matches))))))

;; ============================================================================
;; Prize Calculation Tests
;; ============================================================================

(deftest test-check-ticket
  (testing "Jackpot win returns jackpot amount (no multiplier)"
    (let [ticket (mm/create-ticket [1 2 3 4 5] 10 5)
          drawing (mm/create-drawing [1 2 3 4 5] 10 test-jackpot)
          prize (mm/check-ticket ticket drawing)]
      (is (= test-jackpot prize))))

  (testing "Second prize gets multiplied"
    (let [ticket (mm/create-ticket [1 2 3 4 5] 10 3)
          drawing (mm/create-drawing [1 2 3 4 5] 11 test-jackpot)
          prize (mm/check-ticket ticket drawing)]
      (is (= (* 1000000 3) prize))))

  (testing "Small prize with 2X multiplier"
    (let [ticket (mm/create-ticket [1 2 10 11 12] 5 2)
          drawing (mm/create-drawing [1 2 3 4 5] 5 test-jackpot)
          prize (mm/check-ticket ticket drawing)]
      ;; 2 matches + MB = $7 base × 2 = $14
      (is (= 14 prize))))

  (testing "No match returns 0"
    (let [ticket (mm/create-ticket [1 2 3 4 5] 10 2)
          drawing (mm/create-drawing [60 61 62 63 64] 11 test-jackpot)
          prize (mm/check-ticket ticket drawing)]
      (is (= 0 prize)))))

;; ============================================================================
;; Simulation Tests
;; ============================================================================

(deftest test-simulate-reinvest-strategy
  (testing "Simulation returns required fields"
    (let [result (mm/simulate-reinvest-strategy 25)]
      (is (contains? result :drawings-played))
      (is (contains? result :total-spent))
      (is (contains? result :total-won))
      (is (contains? result :net-result))))

  (testing "With $25 starts with at least 1 drawing"
    (let [result (mm/simulate-reinvest-strategy 25)]
      (is (>= (:drawings-played result) 1))))

  (testing "Net result calculation is correct"
    (let [result (mm/simulate-reinvest-strategy 25)]
      (is (= (:net-result result)
             (+ (- (:total-won result) (:total-spent result))
                (:cash-out-amount result)))))))

(deftest test-run-simulations
  (testing "Run small batch of simulations"
    (let [stats (mm/run-simulations 25 10)]
      (is (= 10 (:simulations stats)))
      (is (= 25 (:initial-dollars stats)))
      (is (= 10 (count (:results stats))))
      (is (contains? (:drawings-stats stats) :min))
      (is (contains? (:drawings-stats stats) :max))
      (is (contains? (:drawings-stats stats) :avg))
      (is (contains? (:drawings-stats stats) :median))))

  (testing "All simulations start with at least 1 drawing"
    (let [stats (mm/run-simulations 25 10)]
      (is (>= (get-in stats [:drawings-stats :min]) 1)))))
