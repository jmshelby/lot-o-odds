(ns jmshelby.lot-o-odds.lucky-for-life.core
  (:require [clojure.set :as set]))

;; ============================================================================
;; Lucky for Life Game Configuration
;; ============================================================================

;; Game Rules:
;; - Pick 5 numbers from 1-48
;; - Pick 1 Lucky Ball from 1-18
;; - Drawings held nightly
;; - Top prize: $1,000 a day for life
;; - Second prize: $25,000 a year for life

(def game-config
  "Lucky for Life game configuration"
  {:main-numbers {:min 1
                  :max 48
                  :count 5}
   :lucky-ball {:min 1
                :max 18
                :count 1}})

;; Prize tier structure
(def prize-tiers
  "Prize tiers with match requirements and odds"
  [{:match [5 1] :prize "$1,000/day for life" :odds [1 30821472]}
   {:match [5 0] :prize "$25,000/year for life" :odds [1 1813028]}
   {:match [4 1] :prize 5000 :odds [1 143356]}
   {:match [4 0] :prize 200 :odds [1 8433]}
   {:match [3 1] :prize 150 :odds [1 3413]}
   {:match [3 0] :prize 20 :odds [1 201]}
   {:match [2 1] :prize 25 :odds [1 250]}
   {:match [2 0] :prize 3 :odds [1 15]}
   {:match [1 1] :prize 6 :odds [1 50]}
   {:match [0 1] :prize 4 :odds [1 32]}])

;; ============================================================================
;; Ticket and Drawing Representation
;; ============================================================================

(defn valid-main-number?
  "Check if a number is valid for main numbers (1-48)"
  [n]
  (and (int? n)
       (>= n (get-in game-config [:main-numbers :min]))
       (<= n (get-in game-config [:main-numbers :max]))))

(defn valid-lucky-ball?
  "Check if a number is valid for the lucky ball (1-18)"
  [n]
  (and (int? n)
       (>= n (get-in game-config [:lucky-ball :min]))
       (<= n (get-in game-config [:lucky-ball :max]))))

(defn create-ticket
  "Create a ticket with 5 main numbers and 1 lucky ball.
  Returns a map with :main-numbers (sorted set) and :lucky-ball."
  [main-numbers lucky-ball]
  {:pre [(= 5 (count main-numbers))
         (every? valid-main-number? main-numbers)
         (valid-lucky-ball? lucky-ball)
         (= 5 (count (distinct main-numbers)))]}
  {:main-numbers (into (sorted-set) main-numbers)
   :lucky-ball lucky-ball})

(defn create-drawing
  "Create a drawing with 5 main numbers and 1 lucky ball.
  Uses the same structure as a ticket."
  [main-numbers lucky-ball]
  (create-ticket main-numbers lucky-ball))

;; ============================================================================
;; Random Generation
;; ============================================================================

(defn generate-random-main-numbers
  "Generate 5 random unique numbers from 1-48"
  []
  (vec (take 5 (shuffle (range (get-in game-config [:main-numbers :min])
                                (inc (get-in game-config [:main-numbers :max])))))))

(defn generate-random-lucky-ball
  "Generate a random lucky ball from 1-18"
  []
  (+ (get-in game-config [:lucky-ball :min])
     (rand-int (get-in game-config [:lucky-ball :max]))))

(defn generate-random-ticket
  "Generate a random ticket with 5 main numbers and 1 lucky ball"
  []
  (create-ticket (generate-random-main-numbers)
                 (generate-random-lucky-ball)))

(defn generate-random-drawing
  "Generate a random drawing with 5 main numbers and 1 lucky ball"
  []
  (create-drawing (generate-random-main-numbers)
                  (generate-random-lucky-ball)))

;; ============================================================================
;; Matching Logic
;; ============================================================================

(defn count-matches
  "Count how many main numbers match between ticket and drawing.
  Returns a map with :main-matches and :lucky-ball-match"
  [ticket drawing]
  (let [main-matches (count (set/intersection
                             (:main-numbers ticket)
                             (:main-numbers drawing)))
        lb-match (if (= (:lucky-ball ticket) (:lucky-ball drawing)) 1 0)]
    {:main-matches main-matches
     :lucky-ball-match lb-match}))

(defn find-prize-tier
  "Find the prize tier for a given match result.
  Returns nil if no prize won."
  [match-result]
  (let [main-matches (:main-matches match-result)
        lb-match (:lucky-ball-match match-result)]
    (first (filter #(= (:match %) [main-matches lb-match]) prize-tiers))))

(defn check-ticket
  "Check a ticket against a drawing and return the prize tier.
  Returns nil if no prize won."
  [ticket drawing]
  (let [matches (count-matches ticket drawing)]
    (find-prize-tier matches)))

;; ============================================================================
;; Prize Values and Economics
;; ============================================================================

(def ticket-price
  "Cost of one Lucky for Life ticket in dollars"
  2)

(defn prize-cash-value
  "Convert a prize to its cash value in dollars.
  Top two prizes use cash option values; others are fixed amounts."
  [prize]
  (case prize
    "$1,000/day for life" 5750000
    "$25,000/year for life" 390000
    prize)) ; For numeric prizes, return as-is

(defn tickets-from-dollars
  "Calculate how many tickets can be purchased with given dollars"
  [dollars]
  (int (/ dollars ticket-price)))

;; ============================================================================
;; Play Strategy Simulations
;; ============================================================================

(defn play-drawing
  "Play a single drawing with multiple tickets.
  Returns total winnings in dollars."
  [tickets drawing]
  (let [results (map #(check-ticket % drawing) tickets)
        prizes (filter some? results)
        prize-values (map :prize prizes)
        cash-values (map prize-cash-value prize-values)]
    (reduce + 0 cash-values)))

(defn simulate-reinvest-strategy
  "Simulate the reinvest-all-winnings strategy until bankruptcy.

  Strategy:
  - Start with initial-dollars
  - Buy as many tickets as possible
  - Play one drawing
  - Reinvest all winnings into new tickets
  - Continue until unable to buy tickets

  Returns a map with:
  - :drawings-played - number of drawings survived
  - :total-spent - total money spent on tickets
  - :total-won - total prize money won
  - :net-result - final profit/loss"
  [initial-dollars]
  (loop [dollars initial-dollars
         drawings-played 0
         total-spent 0
         total-won 0]
    (let [num-tickets (tickets-from-dollars dollars)]
      (if (< num-tickets 1)
        ;; Can't buy any tickets, return results
        {:drawings-played drawings-played
         :total-spent total-spent
         :total-won total-won
         :net-result (- total-won total-spent)}
        ;; Play another drawing
        (let [cost (* num-tickets ticket-price)
              tickets (repeatedly num-tickets generate-random-ticket)
              drawing (generate-random-drawing)
              winnings (play-drawing tickets drawing)
              new-dollars winnings]
          (recur new-dollars
                 (inc drawings-played)
                 (+ total-spent cost)
                 (+ total-won winnings)))))))

(defn run-simulations
  "Run N simulations of the reinvest strategy and return statistics.

  Returns a map with:
  - :simulations - number of simulations run
  - :results - vector of all simulation results
  - :drawings-stats - {:min :max :avg :median} for drawings survived
  - :net-result-stats - {:min :max :avg :median} for net profit/loss"
  [initial-dollars num-simulations]
  (let [results (repeatedly num-simulations #(simulate-reinvest-strategy initial-dollars))
        drawings (map :drawings-played results)
        net-results (map :net-result results)

        stats-for (fn [values]
                    (let [sorted (sort values)
                          count (count sorted)
                          median (nth sorted (quot count 2))]
                      {:min (apply min values)
                       :max (apply max values)
                       :avg (double (/ (reduce + values) count))
                       :median median}))]

    {:simulations num-simulations
     :initial-dollars initial-dollars
     :results (vec results)
     :drawings-stats (stats-for drawings)
     :net-result-stats (stats-for net-results)}))
