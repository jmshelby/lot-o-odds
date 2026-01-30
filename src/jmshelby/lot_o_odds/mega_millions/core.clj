(ns jmshelby.lot-o-odds.mega-millions.core
  (:require [clojure.set :as set]))

;; ============================================================================
;; Mega Millions Game Configuration (New $5 format as of April 2025)
;; ============================================================================

;; Game Rules:
;; - Pick 5 numbers from 1-70
;; - Pick 1 Mega Ball from 1-24
;; - Drawings held Tuesday and Friday nights
;; - Ticket costs $5 (changed from $2 in April 2025)
;; - Built-in multiplier (2X-10X) for all non-jackpot prizes
;; - Multiplier distribution: 15×2X, 10×3X, 4×4X, 2×5X, 1×10X

(def game-config
  "Mega Millions game configuration"
  {:main-numbers {:min 1
                  :max 70
                  :count 5}
   :mega-ball {:min 1
               :max 24
               :count 1}})

(def ticket-price
  "Cost of one Mega Millions ticket in dollars (as of April 2025)"
  5)

;; Multiplier distribution (field of 32 multipliers)
(def multiplier-distribution
  "Distribution of multipliers in the pool of 32"
  (concat (repeat 15 2)
          (repeat 10 3)
          (repeat 4 4)
          (repeat 2 5)
          (repeat 1 10)))

;; Prize tier structure (base prizes before multiplier)
;; Note: All non-jackpot prizes are multiplied (minimum 2X)
(def prize-tiers
  "Prize tiers with match requirements and odds.
  Prizes are base amounts that get multiplied (except jackpot)."
  [{:match [5 1] :base-prize :jackpot :odds [1 290472336]}
   {:match [5 0] :base-prize 1000000 :odds [1 12629232]}
   {:match [4 1] :base-prize 10000 :odds [1 893762]}
   {:match [4 0] :base-prize 500 :odds [1 38860]}
   {:match [3 1] :base-prize 200 :odds [1 13966]}
   {:match [3 0] :base-prize 10 :odds [1 318]}
   {:match [2 1] :base-prize 7 :odds [1 86]}
   {:match [1 1] :base-prize 5 :odds [1 36]}
   {:match [0 1] :base-prize 5 :odds [1 24]}])

;; ============================================================================
;; Ticket and Drawing Representation
;; ============================================================================

(defn valid-main-number?
  "Check if a number is valid for main numbers (1-70)"
  [n]
  (and (int? n)
       (>= n (get-in game-config [:main-numbers :min]))
       (<= n (get-in game-config [:main-numbers :max]))))

(defn valid-mega-ball?
  "Check if a number is valid for the mega ball (1-24)"
  [n]
  (and (int? n)
       (>= n (get-in game-config [:mega-ball :min]))
       (<= n (get-in game-config [:mega-ball :max]))))

(defn create-ticket
  "Create a ticket with 5 main numbers and 1 mega ball.
  Returns a map with :main-numbers (sorted set), :mega-ball, and :multiplier."
  ([main-numbers mega-ball]
   (create-ticket main-numbers mega-ball (rand-nth multiplier-distribution)))
  ([main-numbers mega-ball multiplier]
   {:pre [(= 5 (count main-numbers))
          (every? valid-main-number? main-numbers)
          (valid-mega-ball? mega-ball)
          (= 5 (count (distinct main-numbers)))
          (some #(= multiplier %) multiplier-distribution)]}
   {:main-numbers (into (sorted-set) main-numbers)
    :mega-ball mega-ball
    :multiplier multiplier}))

(defn create-drawing
  "Create a drawing with 5 main numbers, 1 mega ball, and jackpot amount.
  Drawing does not have a multiplier - multipliers are per-ticket."
  [main-numbers mega-ball jackpot-amount]
  {:pre [(= 5 (count main-numbers))
         (every? valid-main-number? main-numbers)
         (valid-mega-ball? mega-ball)
         (= 5 (count (distinct main-numbers)))
         (pos? jackpot-amount)]}
  {:main-numbers (into (sorted-set) main-numbers)
   :mega-ball mega-ball
   :jackpot jackpot-amount})

;; ============================================================================
;; Random Generation
;; ============================================================================

(defn generate-random-main-numbers
  "Generate 5 random unique numbers from 1-70"
  []
  (vec (take 5 (shuffle (range (get-in game-config [:main-numbers :min])
                                (inc (get-in game-config [:main-numbers :max])))))))

(defn generate-random-mega-ball
  "Generate a random mega ball from 1-24"
  []
  (+ (get-in game-config [:mega-ball :min])
     (rand-int (get-in game-config [:mega-ball :max]))))

(defn generate-random-multiplier
  "Generate a random multiplier from the distribution"
  []
  (rand-nth multiplier-distribution))

(defn generate-random-ticket
  "Generate a random ticket with 5 main numbers, 1 mega ball, and multiplier"
  []
  (create-ticket (generate-random-main-numbers)
                 (generate-random-mega-ball)
                 (generate-random-multiplier)))

(defn generate-random-drawing
  "Generate a random drawing with 5 main numbers, 1 mega ball, and jackpot amount"
  ([]
   (generate-random-drawing 50000000)) ; Default $50M jackpot
  ([jackpot-amount]
   (create-drawing (generate-random-main-numbers)
                   (generate-random-mega-ball)
                   jackpot-amount)))

;; ============================================================================
;; Matching Logic
;; ============================================================================

(defn count-matches
  "Count how many main numbers match between ticket and drawing.
  Returns a map with :main-matches and :mega-ball-match"
  [ticket drawing]
  (let [main-matches (count (set/intersection
                             (:main-numbers ticket)
                             (:main-numbers drawing)))
        mb-match (if (= (:mega-ball ticket) (:mega-ball drawing)) 1 0)]
    {:main-matches main-matches
     :mega-ball-match mb-match}))

(defn find-prize-tier
  "Find the prize tier for a given match result.
  Returns nil if no prize won."
  [match-result]
  (let [main-matches (:main-matches match-result)
        mb-match (:mega-ball-match match-result)]
    (first (filter #(= (:match %) [main-matches mb-match]) prize-tiers))))

(defn calculate-prize
  "Calculate the actual prize won including multiplier.
  For jackpot, return the jackpot amount (no multiplier).
  For other prizes, apply the ticket's multiplier."
  [prize-tier ticket drawing]
  (if (= :jackpot (:base-prize prize-tier))
    (:jackpot drawing)
    (* (:base-prize prize-tier) (:multiplier ticket))))

(defn check-ticket
  "Check a ticket against a drawing and return prize won (in dollars).
  Returns 0 if no prize won."
  [ticket drawing]
  (let [matches (count-matches ticket drawing)
        prize-tier (find-prize-tier matches)]
    (if prize-tier
      (calculate-prize prize-tier ticket drawing)
      0)))

;; ============================================================================
;; Prize Values and Economics
;; ============================================================================

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
  (reduce + (map #(check-ticket % drawing) tickets)))

(defn simulate-reinvest-strategy
  "Simulate the reinvest-all-winnings strategy until bankruptcy.

  Strategy:
  - Start with initial-dollars
  - Buy as many tickets as possible (each with random multiplier)
  - Play one drawing with fixed jackpot amount
  - Reinvest all winnings into new tickets
  - Continue until unable to buy tickets

  Returns a map with:
  - :drawings-played - number of drawings survived
  - :total-spent - total money spent on tickets
  - :total-won - total prize money won
  - :net-result - final profit/loss"
  ([initial-dollars]
   (simulate-reinvest-strategy initial-dollars 50000000)) ; Default $50M jackpot
  ([initial-dollars jackpot-amount]
   (loop [dollars initial-dollars
          drawings-played 0
          total-spent 0
          total-won 0]
     (let [num-tickets (tickets-from-dollars dollars)
           cost (* num-tickets ticket-price)
           remaining-after-play (- dollars cost)]
       (cond
         ;; Can't buy any tickets
         (< num-tickets 1)
         {:drawings-played drawings-played
          :total-spent total-spent
          :total-won total-won
          :cash-out-amount dollars
          :net-result (+ (- total-won total-spent) dollars)}

         ;; Safe to play another drawing
         :else
         (let [tickets (repeatedly num-tickets generate-random-ticket)
               drawing (generate-random-drawing jackpot-amount)
               winnings (play-drawing tickets drawing)
               new-dollars (+ remaining-after-play winnings)]
           (recur new-dollars
                  (inc drawings-played)
                  (+ total-spent cost)
                  (+ total-won winnings))))))))

(defn run-simulations
  "Run N simulations of the reinvest strategy in parallel and return statistics.

  Each simulation is run independently on separate threads for improved performance.

  Returns a map with:
  - :simulations - number of simulations run
  - :initial-dollars - initial investment amount
  - :results - vector of all simulation results
  - :drawings-stats - {:min :max :avg :median} for drawings survived
  - :net-result-stats - {:min :max :avg :median} for net profit/loss"
  ([initial-dollars num-simulations]
   (run-simulations initial-dollars num-simulations 50000000))
  ([initial-dollars num-simulations jackpot-amount]
   (let [results (doall (pmap (fn [_] (simulate-reinvest-strategy initial-dollars jackpot-amount))
                               (range num-simulations)))
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
      :jackpot-amount jackpot-amount
      :results (vec results)
      :drawings-stats (stats-for drawings)
      :net-result-stats (stats-for net-results)})))
