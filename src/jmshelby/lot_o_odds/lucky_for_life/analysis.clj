(ns jmshelby.lot-o-odds.lucky-for-life.analysis
  (:require [jmshelby.lot-o-odds.lucky-for-life.core :as lfl]))

;; ============================================================================
;; Expected Value Analysis
;; ============================================================================

(def prize-odds
  "Prize values (cash) with their odds [numerator denominator]"
  [[5750000 [1 30821472]]    ; Top prize - $1000/day for life (cash value)
   [390000 [1 1813028]]      ; Second prize - $25000/year for life (cash value)
   [5000 [1 143356]]         ; 4 + LB
   [200 [1 8433]]            ; 4 of 5
   [150 [1 3413]]            ; 3 + LB
   [20 [1 201]]              ; 3 of 5
   [25 [1 250]]              ; 2 + LB
   [3 [1 15]]                ; 2 of 5
   [6 [1 50]]                ; 1 + LB
   [4 [1 32]]])              ; LB only

(defn calculate-expected-value
  "Calculate the expected value of a single Lucky for Life ticket.

  Returns a map with:
  - :ticket-cost - cost of one ticket ($2)
  - :expected-value - expected return per ticket
  - :expected-loss - expected loss per ticket
  - :loss-percentage - percentage of ticket cost lost
  - :breakdowns - vector of per-prize EV contributions"
  []
  (let [ticket-cost lfl/ticket-price

        breakdowns (mapv (fn [[prize [num denom]]]
                          (let [probability (/ (double num) denom)
                                ev-contribution (* prize probability)]
                            {:prize prize
                             :odds [num denom]
                             :probability probability
                             :ev-contribution ev-contribution}))
                        prize-odds)

        total-ev (reduce + (map :ev-contribution breakdowns))
        expected-loss (- ticket-cost total-ev)
        loss-pct (* 100 (/ expected-loss ticket-cost))]

    {:ticket-cost ticket-cost
     :expected-value total-ev
     :expected-loss expected-loss
     :loss-percentage loss-pct
     :breakdowns breakdowns}))

(defn print-expected-value-analysis
  "Pretty print the expected value analysis"
  []
  (let [analysis (calculate-expected-value)]
    (println "\n=== LUCKY FOR LIFE EXPECTED VALUE ANALYSIS ===\n")
    (println "Prize         | Odds              | Probability      | EV Contribution")
    (println "--------------|-------------------|------------------|----------------")
    (doseq [{:keys [prize odds probability ev-contribution]} (:breakdowns analysis)]
      (printf "$%-12s | 1 in %,12d | %16.10f | $%.6f\n"
              (format "%,d" prize)
              (second odds)
              probability
              ev-contribution))
    (println)
    (printf "Ticket Cost:              $%.2f\n" (double (:ticket-cost analysis)))
    (printf "Expected Value:           $%.4f\n" (:expected-value analysis))
    (printf "Expected Loss per ticket: $%.4f (%.2f%%)\n"
            (:expected-loss analysis)
            (:loss-percentage analysis))
    (println)
    (printf "For every $100 spent, you expect to get back: $%.2f\n"
            (* 50 (:expected-value analysis)))
    (printf "For every $100 spent, you expect to LOSE:     $%.2f\n"
            (* 50 (:expected-loss analysis)))
    (println)
    (println "CONCLUSION:")
    (println "Every ticket has negative expected value.")
    (println "No amount of starting money gives you an advantage -")
    (println "you'll lose the same percentage regardless of how much you start with!")
    (println)))
