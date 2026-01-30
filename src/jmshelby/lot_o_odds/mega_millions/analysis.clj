(ns jmshelby.lot-o-odds.mega-millions.analysis
  (:require [jmshelby.lot-o-odds.mega-millions.core :as mm]))

;; ============================================================================
;; Expected Value Analysis for Mega Millions
;; ============================================================================

(def prize-odds-with-multiplier
  "Prize values (base) with their odds.
  Note: Non-jackpot prizes are multiplied by 2X-10X"
  [[1000000 [1 12629232]]     ; 5 + no MB (always gets multiplied)
   [10000 [1 893762]]          ; 4 + MB
   [500 [1 38860]]             ; 4 + no MB
   [200 [1 13966]]             ; 3 + MB
   [10 [1 318]]                ; 3 + no MB
   [7 [1 86]]                  ; 2 + MB
   [5 [1 36]]                  ; 1 + MB
   [5 [1 24]]])                ; 0 + MB

(def multiplier-probabilities
  "Probabilities for each multiplier value (out of 32)"
  {2 (/ 15 32)
   3 (/ 10 32)
   4 (/ 4 32)
   5 (/ 2 32)
   10 (/ 1 32)})

(defn calculate-expected-multiplier
  "Calculate the expected value of the multiplier"
  []
  (reduce + (map (fn [[mult prob]] (* mult prob)) multiplier-probabilities)))

(defn calculate-expected-value
  "Calculate the expected value of a single Mega Millions ticket.

  For the jackpot, we use a fixed amount (default $50M cash value).
  For other prizes, we calculate EV with average multiplier.

  Returns a map with:
  - :ticket-cost - cost of one ticket ($5)
  - :jackpot-assumption - jackpot amount used in calculation
  - :expected-value - expected return per ticket
  - :expected-loss - expected loss per ticket
  - :loss-percentage - percentage of ticket cost lost
  - :breakdowns - vector of per-prize EV contributions"
  ([]
   (calculate-expected-value 50000000)) ; Default $50M jackpot
  ([jackpot-amount]
   (let [ticket-cost mm/ticket-price
         avg-multiplier (calculate-expected-multiplier)

         ;; Jackpot contribution (no multiplier)
         jackpot-ev (* jackpot-amount (/ 1 290472336))

         ;; Other prizes with average multiplier
         breakdowns (into [{:prize "Jackpot"
                           :base-prize jackpot-amount
                           :multiplied-prize jackpot-amount
                           :odds [1 290472336]
                           :probability (/ 1.0 290472336)
                           :ev-contribution jackpot-ev}]
                         (mapv (fn [[base-prize [num denom]]]
                                (let [probability (/ (double num) denom)
                                      avg-prize (* base-prize avg-multiplier)
                                      ev-contribution (* avg-prize probability)]
                                  {:prize (str "$" base-prize " base")
                                   :base-prize base-prize
                                   :multiplied-prize avg-prize
                                   :odds [num denom]
                                   :probability probability
                                   :ev-contribution ev-contribution}))
                              prize-odds-with-multiplier))

         total-ev (reduce + (map :ev-contribution breakdowns))
         expected-loss (- ticket-cost total-ev)
         loss-pct (* 100 (/ expected-loss ticket-cost))]

     {:ticket-cost ticket-cost
      :jackpot-assumption jackpot-amount
      :expected-value total-ev
      :expected-loss expected-loss
      :loss-percentage loss-pct
      :avg-multiplier avg-multiplier
      :breakdowns breakdowns})))

(defn print-expected-value-analysis
  "Pretty print the expected value analysis"
  ([]
   (print-expected-value-analysis 50000000))
  ([jackpot-amount]
   (let [analysis (calculate-expected-value jackpot-amount)]
     (println "\n=== MEGA MILLIONS EXPECTED VALUE ANALYSIS ===\n")
     (printf "Jackpot Amount (cash value): $%,d\n" (long jackpot-amount))
     (printf "Average Multiplier: %.2fX\n\n" (double (:avg-multiplier analysis)))
     (println "Prize         | Base    | Avg Multiplied | Odds              | Probability      | EV Contribution")
     (println "--------------|---------|----------------|-------------------|------------------|----------------")
     (doseq [{:keys [prize base-prize multiplied-prize odds probability ev-contribution]} (:breakdowns analysis)]
       (if (= prize "Jackpot")
         (printf "%-13s | $%,6d | $%,12d | 1 in %,12d | %16.10f | $%.6f\n"
                 prize base-prize (long multiplied-prize) (second odds) (double probability) (double ev-contribution))
         (printf "%-13s | $%,6d | $%,12.2f | 1 in %,12d | %16.10f | $%.6f\n"
                 prize base-prize (double multiplied-prize) (second odds) (double probability) (double ev-contribution))))
     (println)
     (printf "Ticket Cost:              $%.2f\n" (double (:ticket-cost analysis)))
     (printf "Expected Value:           $%.4f\n" (double (:expected-value analysis)))
     (printf "Expected Loss per ticket: $%.4f (%.2f%%)\n"
             (double (:expected-loss analysis))
             (double (:loss-percentage analysis)))
     (println)
     (printf "For every $100 spent, you expect to get back: $%.2f\n"
             (double (* 20 (:expected-value analysis))))
     (printf "For every $100 spent, you expect to LOSE:     $%.2f\n"
             (double (* 20 (:expected-loss analysis))))
     (println)
     (println "CONCLUSION:")
     (printf "With a $%,d jackpot, each ticket loses $%.2f (%.1f%%) on average.\n"
             jackpot-amount
             (:expected-loss analysis)
             (:loss-percentage analysis))
     (println "The built-in multiplier helps, but the lottery still has massive house edge!")
     (println))))
