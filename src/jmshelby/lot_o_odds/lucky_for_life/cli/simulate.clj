(ns jmshelby.lot-o-odds.lucky-for-life.cli.simulate
  (:require [jmshelby.lot-o-odds.lucky-for-life.core :as lfl]))

;; ============================================================================
;; CLI for Running Simulations
;; ============================================================================

(defn print-stats
  "Pretty print simulation statistics"
  [stats]
  (println "\n=== LUCKY FOR LIFE SIMULATION RESULTS ===")
  (println "Simulations run:    " (:simulations stats))
  (println "Initial investment: $" (:initial-dollars stats))
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
  "Run reinvest strategy simulations from CLI.

  Usage:
    clojure -M:simulate [initial-dollars] [num-simulations]

  Defaults:
    initial-dollars: 20
    num-simulations: 100"
  [& args]
  (let [initial-dollars (if (first args)
                          (Integer/parseInt (first args))
                          20)
        num-simulations (if (second args)
                          (Integer/parseInt (second args))
                          100)]
    (println "\nRunning" num-simulations "simulations starting with $" initial-dollars "...")
    (let [stats (lfl/run-simulations initial-dollars num-simulations)]
      (print-stats stats))
    (shutdown-agents)))
