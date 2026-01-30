(ns jmshelby.lot-o-odds.mega-millions.cli.simulate
  (:require [jmshelby.lot-o-odds.mega-millions.core :as mm]))

;; ============================================================================
;; CLI for Running Mega Millions Simulations
;; ============================================================================

(defn print-stats
  "Pretty print simulation statistics"
  [stats]
  (println "\n=== MEGA MILLIONS SIMULATION RESULTS ===")
  (println "Simulations run:    " (:simulations stats))
  (println "Initial investment: $" (:initial-dollars stats))
  (println "Jackpot amount:     $" (:jackpot-amount stats))
  (println)
  (println "Drawings Survived:")
  (println "  Min:    " (get-in stats [:drawings-stats :min]))
  (println "  Max:    " (get-in stats [:drawings-stats :max]))
  (println "  Average:" (format "%.2f" (get-in stats [:drawings-stats :avg])))
  (println "  Median: " (get-in stats [:drawings-stats :median]))
  (println)
  (println "Net Result (Profit/Loss):")
  (println "  Min:    $" (get-in stats [:net-result-stats :min]))
  (println "  Max:    $" (get-in stats [:net-result-stats :max]))
  (println "  Average:$" (format "%.2f" (get-in stats [:net-result-stats :avg])))
  (println "  Median: $" (get-in stats [:net-result-stats :median]))
  (println)
  (println "Sample Results (first 10):")
  (doseq [result (take 10 (:results stats))]
    (println (format "  Drawings: %3d | Spent: $%7d | Won: $%7d | Net: $%8d"
                     (:drawings-played result)
                     (:total-spent result)
                     (:total-won result)
                     (:net-result result))))
  (println))

(defn -main
  "Run reinvest strategy simulations for Mega Millions from CLI.

  Usage:
    clojure -M:mm-simulate [initial-dollars] [num-simulations] [jackpot]

  Defaults:
    initial-dollars: 25 ($5 ticket × 5)
    num-simulations: 100
    jackpot: 50000000 ($50M)"
  [& args]
  (let [initial-dollars (if (first args)
                          (Integer/parseInt (first args))
                          25)
        num-simulations (if (second args)
                          (Integer/parseInt (second args))
                          100)
        jackpot-amount (if (nth args 2 nil)
                         (Long/parseLong (nth args 2))
                         50000000)]
    (println "\nRunning" num-simulations "simulations starting with $" initial-dollars "...")
    (println "Jackpot amount: $" jackpot-amount)
    (let [stats (mm/run-simulations initial-dollars num-simulations jackpot-amount)]
      (print-stats stats))
    (shutdown-agents)))
