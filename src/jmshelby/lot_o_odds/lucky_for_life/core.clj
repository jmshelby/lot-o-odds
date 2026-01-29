(ns jmshelby.lot-o-odds.lucky-for-life.core)

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
;; Matching Logic
;; ============================================================================

(defn count-matches
  "Count how many main numbers match between ticket and drawing.
  Returns a map with :main-matches and :lucky-ball-match"
  [ticket drawing]
  (let [main-matches (count (clojure.set/intersection
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
