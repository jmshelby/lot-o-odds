(ns jmshelby.lot-o-odds.lucky-for-life.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [jmshelby.lot-o-odds.lucky-for-life.core :as lfl]))

;; ============================================================================
;; Validation Tests
;; ============================================================================

(deftest test-valid-main-number
  (testing "Valid main numbers (1-48)"
    (is (true? (lfl/valid-main-number? 1)))
    (is (true? (lfl/valid-main-number? 24)))
    (is (true? (lfl/valid-main-number? 48))))

  (testing "Invalid main numbers"
    (is (false? (lfl/valid-main-number? 0)))
    (is (false? (lfl/valid-main-number? 49)))
    (is (false? (lfl/valid-main-number? -5)))
    (is (false? (lfl/valid-main-number? 1.5)))))

(deftest test-valid-lucky-ball
  (testing "Valid lucky ball numbers (1-18)"
    (is (true? (lfl/valid-lucky-ball? 1)))
    (is (true? (lfl/valid-lucky-ball? 9)))
    (is (true? (lfl/valid-lucky-ball? 18))))

  (testing "Invalid lucky ball numbers"
    (is (false? (lfl/valid-lucky-ball? 0)))
    (is (false? (lfl/valid-lucky-ball? 19)))
    (is (false? (lfl/valid-lucky-ball? -1)))
    (is (false? (lfl/valid-lucky-ball? 5.5)))))

;; ============================================================================
;; Ticket Creation Tests
;; ============================================================================

(deftest test-create-ticket
  (testing "Create valid ticket"
    (let [ticket (lfl/create-ticket [5 12 23 34 45] 7)]
      (is (= #{5 12 23 34 45} (:main-numbers ticket)))
      (is (= 7 (:lucky-ball ticket)))))

  (testing "Ticket numbers are sorted"
    (let [ticket (lfl/create-ticket [45 5 23 12 34] 7)]
      (is (= #{5 12 23 34 45} (:main-numbers ticket)))))

  (testing "Invalid ticket - wrong count"
    (is (thrown? AssertionError (lfl/create-ticket [1 2 3] 5))))

  (testing "Invalid ticket - duplicate numbers"
    (is (thrown? AssertionError (lfl/create-ticket [1 2 3 4 4] 5))))

  (testing "Invalid ticket - out of range main number"
    (is (thrown? AssertionError (lfl/create-ticket [1 2 3 4 50] 5))))

  (testing "Invalid ticket - out of range lucky ball"
    (is (thrown? AssertionError (lfl/create-ticket [1 2 3 4 5] 20)))))

;; ============================================================================
;; Matching Tests
;; ============================================================================

(deftest test-count-matches
  (testing "Perfect match - all 5 + lucky ball"
    (let [ticket (lfl/create-ticket [1 2 3 4 5] 10)
          drawing (lfl/create-drawing [1 2 3 4 5] 10)
          matches (lfl/count-matches ticket drawing)]
      (is (= 5 (:main-matches matches)))
      (is (= 1 (:lucky-ball-match matches)))))

  (testing "5 main numbers, no lucky ball"
    (let [ticket (lfl/create-ticket [1 2 3 4 5] 10)
          drawing (lfl/create-drawing [1 2 3 4 5] 11)
          matches (lfl/count-matches ticket drawing)]
      (is (= 5 (:main-matches matches)))
      (is (= 0 (:lucky-ball-match matches)))))

  (testing "Partial match - 3 main + lucky ball"
    (let [ticket (lfl/create-ticket [1 2 3 10 11] 5)
          drawing (lfl/create-drawing [1 2 3 20 21] 5)
          matches (lfl/count-matches ticket drawing)]
      (is (= 3 (:main-matches matches)))
      (is (= 1 (:lucky-ball-match matches)))))

  (testing "Only lucky ball matches"
    (let [ticket (lfl/create-ticket [1 2 3 4 5] 10)
          drawing (lfl/create-drawing [10 20 30 40 48] 10)
          matches (lfl/count-matches ticket drawing)]
      (is (= 0 (:main-matches matches)))
      (is (= 1 (:lucky-ball-match matches)))))

  (testing "No matches"
    (let [ticket (lfl/create-ticket [1 2 3 4 5] 10)
          drawing (lfl/create-drawing [10 20 30 40 48] 11)
          matches (lfl/count-matches ticket drawing)]
      (is (= 0 (:main-matches matches)))
      (is (= 0 (:lucky-ball-match matches))))))

;; ============================================================================
;; Prize Tier Tests
;; ============================================================================

(deftest test-find-prize-tier
  (testing "Top prize - 5 + LB"
    (let [tier (lfl/find-prize-tier {:main-matches 5 :lucky-ball-match 1})]
      (is (= "$1,000/day for life" (:prize tier)))
      (is (= [1 30821472] (:odds tier)))))

  (testing "Second prize - 5 of 5"
    (let [tier (lfl/find-prize-tier {:main-matches 5 :lucky-ball-match 0})]
      (is (= "$25,000/year for life" (:prize tier)))))

  (testing "Small prize - 2 matches"
    (let [tier (lfl/find-prize-tier {:main-matches 2 :lucky-ball-match 0})]
      (is (= 3 (:prize tier)))))

  (testing "No prize"
    (let [tier (lfl/find-prize-tier {:main-matches 0 :lucky-ball-match 0})]
      (is (nil? tier)))))

(deftest test-check-ticket
  (testing "Winning ticket - jackpot"
    (let [ticket (lfl/create-ticket [1 2 3 4 5] 10)
          drawing (lfl/create-drawing [1 2 3 4 5] 10)
          result (lfl/check-ticket ticket drawing)]
      (is (= "$1,000/day for life" (:prize result)))))

  (testing "Winning ticket - small prize"
    (let [ticket (lfl/create-ticket [1 2 3 10 11] 5)
          drawing (lfl/create-drawing [1 2 3 20 21] 5)
          result (lfl/check-ticket ticket drawing)]
      (is (= 150 (:prize result)))))

  (testing "Losing ticket"
    (let [ticket (lfl/create-ticket [1 2 3 4 5] 10)
          drawing (lfl/create-drawing [10 20 30 40 48] 11)
          result (lfl/check-ticket ticket drawing)]
      (is (nil? result)))))

;; ============================================================================
;; Random Generation Tests
;; ============================================================================

(deftest test-generate-random-main-numbers
  (testing "Generates 5 numbers"
    (let [numbers (lfl/generate-random-main-numbers)]
      (is (= 5 (count numbers)))))

  (testing "All numbers are unique"
    (let [numbers (lfl/generate-random-main-numbers)]
      (is (= 5 (count (distinct numbers))))))

  (testing "All numbers are in valid range (1-48)"
    (let [numbers (lfl/generate-random-main-numbers)]
      (is (every? #(and (>= % 1) (<= % 48)) numbers)))))

(deftest test-generate-random-lucky-ball
  (testing "Lucky ball is in valid range (1-18)"
    (dotimes [_ 20]
      (let [lb (lfl/generate-random-lucky-ball)]
        (is (>= lb 1))
        (is (<= lb 18))))))

(deftest test-generate-random-ticket
  (testing "Generates valid ticket"
    (let [ticket (lfl/generate-random-ticket)]
      (is (contains? ticket :main-numbers))
      (is (contains? ticket :lucky-ball))
      (is (= 5 (count (:main-numbers ticket))))
      (is (lfl/valid-lucky-ball? (:lucky-ball ticket)))))

  (testing "Generated tickets have unique numbers"
    (dotimes [_ 10]
      (let [ticket (lfl/generate-random-ticket)]
        (is (= 5 (count (:main-numbers ticket))))))))

(deftest test-generate-random-drawing
  (testing "Generates valid drawing"
    (let [drawing (lfl/generate-random-drawing)]
      (is (contains? drawing :main-numbers))
      (is (contains? drawing :lucky-ball))
      (is (= 5 (count (:main-numbers drawing))))
      (is (lfl/valid-lucky-ball? (:lucky-ball drawing)))))

  (testing "Can check random ticket against random drawing"
    (let [ticket (lfl/generate-random-ticket)
          drawing (lfl/generate-random-drawing)]
      ;; Should not throw an error
      (lfl/check-ticket ticket drawing)
      (is true))))

;; ============================================================================
;; Prize Economics Tests
;; ============================================================================

(deftest test-prize-cash-value
  (testing "Top prize cash value"
    (is (= 5750000 (lfl/prize-cash-value "$1,000/day for life"))))

  (testing "Second prize cash value"
    (is (= 390000 (lfl/prize-cash-value "$25,000/year for life"))))

  (testing "Fixed dollar prizes"
    (is (= 5000 (lfl/prize-cash-value 5000)))
    (is (= 200 (lfl/prize-cash-value 200)))
    (is (= 3 (lfl/prize-cash-value 3)))))

(deftest test-tickets-from-dollars
  (testing "Convert dollars to tickets"
    (is (= 10 (lfl/tickets-from-dollars 20)))
    (is (= 5 (lfl/tickets-from-dollars 10)))
    (is (= 1 (lfl/tickets-from-dollars 2))))

  (testing "Partial dollars rounds down"
    (is (= 0 (lfl/tickets-from-dollars 1)))
    (is (= 5 (lfl/tickets-from-dollars 11)))
    (is (= 10 (lfl/tickets-from-dollars 21)))))

;; ============================================================================
;; Simulation Tests
;; ============================================================================

(deftest test-play-drawing
  (testing "No winners returns 0"
    (let [tickets [(lfl/create-ticket [1 2 3 4 5] 1)]
          drawing (lfl/create-drawing [10 20 30 40 48] 18)
          winnings (lfl/play-drawing tickets drawing)]
      (is (= 0 winnings))))

  (testing "Single small winner"
    (let [tickets [(lfl/create-ticket [1 2 3 4 5] 10)]
          drawing (lfl/create-drawing [1 2 20 30 40] 11)
          winnings (lfl/play-drawing tickets drawing)]
      (is (= 3 winnings)))) ; 2 matches = $3

  (testing "Multiple tickets can win"
    (let [tickets [(lfl/create-ticket [1 2 3 4 5] 10)
                   (lfl/create-ticket [1 2 3 20 21] 10)]
          drawing (lfl/create-drawing [1 2 3 30 40] 11)
          winnings (lfl/play-drawing tickets drawing)]
      (is (= 40 winnings))))) ; Both get 3 matches = $20 each

(deftest test-simulate-reinvest-strategy
  (testing "Simulation returns required fields"
    (let [result (lfl/simulate-reinvest-strategy 20)]
      (is (contains? result :drawings-played))
      (is (contains? result :total-spent))
      (is (contains? result :total-won))
      (is (contains? result :net-result))))

  (testing "With $20 starts with at least 1 drawing"
    (let [result (lfl/simulate-reinvest-strategy 20)]
      (is (>= (:drawings-played result) 1))))

  (testing "Net result is total won minus total spent"
    (let [result (lfl/simulate-reinvest-strategy 20)]
      (is (= (:net-result result)
             (- (:total-won result) (:total-spent result))))))

  (testing "Total spent is always >= initial investment"
    (let [result (lfl/simulate-reinvest-strategy 20)]
      (is (>= (:total-spent result) 20)))))

(deftest test-run-simulations
  (testing "Run small batch of simulations"
    (let [stats (lfl/run-simulations 20 10)]
      (is (= 10 (:simulations stats)))
      (is (= 20 (:initial-dollars stats)))
      (is (= 10 (count (:results stats))))
      (is (contains? (:drawings-stats stats) :min))
      (is (contains? (:drawings-stats stats) :max))
      (is (contains? (:drawings-stats stats) :avg))
      (is (contains? (:drawings-stats stats) :median))))

  (testing "All simulations start with at least 1 drawing"
    (let [stats (lfl/run-simulations 20 10)]
      (is (>= (get-in stats [:drawings-stats :min]) 1))))

  (testing "Stats are reasonable"
    (let [stats (lfl/run-simulations 20 10)]
      (is (<= (get-in stats [:drawings-stats :min])
              (get-in stats [:drawings-stats :avg])
              (get-in stats [:drawings-stats :max]))))))

;; ============================================================================
;; Parallel Execution Tests
;; ============================================================================

(deftest test-run-simulations-parallel
  (testing "Parallel simulations produce valid results"
    (let [stats1 (lfl/run-simulations 20 100)
          stats2 (lfl/run-simulations 20 100)]
      ;; Both should produce 100 results
      (is (= 100 (count (:results stats1))))
      (is (= 100 (count (:results stats2))))
      ;; Stats should be in valid ranges (not testing exact equality due to randomness)
      (is (pos? (get-in stats1 [:drawings-stats :min])))
      (is (pos? (get-in stats2 [:drawings-stats :min])))
      ;; Min should be <= median <= max
      (is (<= (get-in stats1 [:drawings-stats :min])
              (get-in stats1 [:drawings-stats :median])
              (get-in stats1 [:drawings-stats :max])))))

  (testing "Can handle larger parallel batch"
    (let [stats (lfl/run-simulations 20 1000)]
      (is (= 1000 (:simulations stats)))
      (is (= 1000 (count (:results stats))))
      ;; All simulations should have played at least 1 drawing
      (is (every? #(>= (:drawings-played %) 1) (:results stats)))))

  (testing "Parallel execution maintains data integrity"
    (let [stats (lfl/run-simulations 20 50)]
      ;; Verify all results have correct structure
      (is (every? #(contains? % :drawings-played) (:results stats)))
      (is (every? #(contains? % :total-spent) (:results stats)))
      (is (every? #(contains? % :total-won) (:results stats)))
      (is (every? #(contains? % :net-result) (:results stats)))
      ;; Verify net result calculation is correct for all results
      (is (every? #(= (:net-result %)
                      (- (:total-won %) (:total-spent %)))
                  (:results stats))))))

;; ============================================================================
;; Threshold-Based Strategy Tests
;; ============================================================================

(deftest test-simulate-reinvest-with-threshold
  (testing "Simulation with threshold returns required fields"
    (let [result (lfl/simulate-reinvest-with-threshold 20 10)]
      (is (contains? result :drawings-played))
      (is (contains? result :total-spent))
      (is (contains? result :total-won))
      (is (contains? result :cash-out-amount))
      (is (contains? result :net-result))))

  (testing "Stops when below threshold"
    ;; With threshold of $18, should stop after first drawing if any money lost
    (let [result (lfl/simulate-reinvest-with-threshold 20 18)]
      ;; Cash out amount should be >= 0
      (is (>= (:cash-out-amount result) 0))))

  (testing "Threshold of 0 behaves like original strategy"
    (let [result (lfl/simulate-reinvest-with-threshold 20 0)]
      ;; Should play until can't buy tickets
      (is (>= (:drawings-played result) 1))
      ;; Net result accounts for cash-out
      (is (= (:net-result result)
             (+ (- (:total-won result) (:total-spent result))
                (:cash-out-amount result))))))

  (testing "High threshold stops immediately"
    (let [result (lfl/simulate-reinvest-with-threshold 20 20)]
      ;; Should stop immediately without playing
      (is (= 0 (:drawings-played result)))
      (is (= 0 (:total-spent result)))
      (is (= 0 (:total-won result)))
      (is (= 20 (:cash-out-amount result)))
      (is (= 20 (:net-result result))))))

(deftest test-compare-stopping-thresholds
  (testing "Comparison returns required structure"
    (let [comparison (lfl/compare-stopping-thresholds 20 10)]
      (is (= 20 (:initial-dollars comparison)))
      (is (= 10 (:simulations-per-threshold comparison)))
      (is (vector? (:thresholds comparison)))
      (is (pos? (count (:thresholds comparison))))))

  (testing "Each threshold result has required fields"
    (let [comparison (lfl/compare-stopping-thresholds 20 10)
          first-threshold (first (:thresholds comparison))]
      (is (contains? first-threshold :threshold))
      (is (contains? first-threshold :avg-net-result))
      (is (contains? first-threshold :avg-cash-out))
      (is (contains? first-threshold :avg-drawings))
      (is (contains? first-threshold :win-rate))))

  (testing "Threshold of 20 should have best net result"
    (let [comparison (lfl/compare-stopping-thresholds 20 50)
          threshold-20 (first (filter #(= 20 (:threshold %)) (:thresholds comparison)))]
      ;; Stopping immediately means you keep all $20
      (is (= 20.0 (:avg-net-result threshold-20)))
      (is (= 20.0 (:avg-cash-out threshold-20)))
      (is (= 0.0 (:avg-drawings threshold-20))))))

(deftest test-find-best-threshold
  (testing "Finds threshold with highest avg net result"
    (let [comparison (lfl/compare-stopping-thresholds 20 50)
          best (lfl/find-best-threshold comparison)]
      (is (contains? best :threshold))
      (is (contains? best :avg-net-result))
      ;; Best threshold should be 20 (never play)
      (is (= 20 (:threshold best)))
      (is (= 20.0 (:avg-net-result best))))))
