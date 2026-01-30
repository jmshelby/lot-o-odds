(ns jmshelby.lot-o-odds.lucky-for-life.cli.compare-games
  (:require [jmshelby.lot-o-odds.lucky-for-life.analysis :as analysis]))

;; ============================================================================
;; Casino Game House Edge Comparison
;; ============================================================================

(def casino-games
  "House edge percentages for popular casino games"
  [{:game "Blackjack (perfect strategy)" :house-edge 0.43 :source "wizardofodds.com"}
   {:game "Craps (Pass Line)" :house-edge 1.41 :source "wizardofodds.com"}
   {:game "Baccarat (Banker bet)" :house-edge 1.06 :source "wizardofodds.com"}
   {:game "Roulette (European)" :house-edge 2.70 :source "wizardofodds.com"}
   {:game "Roulette (American)" :house-edge 5.26 :source "wizardofodds.com"}
   {:game "Slots (typical)" :house-edge 8.00 :source "casino.org"}
   {:game "Keno (typical)" :house-edge 25.00 :source "casino.org"}])

(defn compare-to-casino-games
  "Compare Lucky for Life to traditional casino games"
  []
  (let [lfl-analysis (analysis/calculate-expected-value)
        lfl-house-edge (:loss-percentage lfl-analysis)

        all-games (conj casino-games
                       {:game "Lucky for Life Lottery"
                        :house-edge lfl-house-edge
                        :source "calculated from prize structure"})

        sorted-games (sort-by :house-edge all-games)]

    (println "\n=== HOUSE EDGE COMPARISON: LUCKY FOR LIFE vs CASINO GAMES ===\n")
    (println "Game                           | House Edge | Expected Loss per $100")
    (println "-------------------------------|------------|------------------------")
    (doseq [{:keys [game house-edge]} sorted-games]
      (printf "%-30s | %9.2f%% | $%.2f\n"
              game
              house-edge
              house-edge))

    (println)
    (let [worst-casino (apply max-key :house-edge casino-games)
          times-worse (/ lfl-house-edge (:house-edge worst-casino))]
      (printf "Lucky for Life is %.1fX WORSE than %s!\n"
              times-worse
              (:game worst-casino)))

    (let [best-casino (apply min-key :house-edge casino-games)
          times-worse (/ lfl-house-edge (:house-edge best-casino))]
      (printf "Lucky for Life is %.0fX WORSE than %s!\n\n"
              times-worse
              (:game best-casino)))

    (println "VISUALIZATION:")
    (println "For every $100 you wager...\n")
    (doseq [{:keys [game house-edge]} (take 4 sorted-games)]
      (let [keep (* (- 100 house-edge))
            lose house-edge]
        (printf "%-30s → Keep $%5.2f | Lose $%5.2f\n" game keep lose)))
    (println "...")
    (let [{:keys [game house-edge]} (last sorted-games)
          keep (- 100 house-edge)
          lose house-edge]
      (printf "%-30s → Keep $%5.2f | Lose $%5.2f ⚠️\n" game keep lose))

    (println)
    (println "CONCLUSION:")
    (println "Lotteries like Lucky for Life are BY FAR the worst gambling value.")
    (println "They exist as a voluntary tax, not as a reasonable way to gamble!")
    (println)))

(defn -main
  "Compare Lucky for Life to casino games"
  [& args]
  (compare-to-casino-games)
  (shutdown-agents))
