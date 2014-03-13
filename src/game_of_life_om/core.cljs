(ns game-of-life-om.core
  (:require-macros [dommy.macros :refer [node sel sel1]]
                   [cljs.core.async.macros :refer [go]])
  (:require [game-of-life-om.cgrand :refer [step]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [dommy.core :as dommy]
            [cljs.core.async :refer [chan put! <!]]))

(enable-console-print!)

(def size 48)

(def ESCAPE_KEY 27)

(def SPACE_BAR 32)

(def ENTER 13)

(def BACK 8)
;;============ DOM related =================================

;;;;;;;;;;; Mark ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn mark!
  "Mark in pink all the divs by direct DOM manipulation."
  []
  (doseq [el (sel [:#visualizer :div])]
    (dommy/add-class! el :marker)))

(dommy/listen! (sel1 :button.mark)
                :click mark!)


;;;;;;;;;;;;;;;Coords Utils ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ->loc
  "Give the absolute position in one dimension."
  [[y x]]
  (+ x (* size y)))

(defn ->cell
  "Give the cell coord given the absolute position"
  [l]
  ((juxt quot rem) l size))

;;;;;;;;;;;;;;;; States ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def predefined {:pentadecathlon #{[15 19] [15 18] [15 17] [15 16] [15 15] [15 14] [15 13] [15 12] [15 11] [15 20]}
                 :pulsar #{[11 14] [13 13] [14 14] [12 13] [14 15] [15 14] [12 14] [13 15] [12 15] [14 13]}
                 :space-schip-1 #{[15 14] [14 13] [14 10] [16 10] [17 12] [17 11] [17 14] [16 14] [17 13]}
                 :glider-gun #{[12 16] [15 19] [16 19] [14 19] [15 17] [13 18] [15 13] [12 15] [13 14] [14 13] [16 27] [13 37] [12 37] [12 38] [14 4] [13 38] [15 4] [15 3] [14 3] [12 24] [15 27] [13 24] [14 24] [15 25] [16 13] [17 14] [10 27] [11 27] [14 23] [13 23] [17 18] [18 15] [11 25] [12 23] [15 20] [18 16]}
                 :organic #{[14 13] [14 14] [16 13] [16 14] [15 15] [15 12] [13 15]}
                 :organic-2 #{[14 13] [14 14] [15 15] [13 15] [15 12] [16 13] [16 14] [17 12]}
                 :T->blinker #{[11 12] [12 12] [13 12] [12 13]}
                 :blinker #{[12 12] [14 10] [11 6] [14 8] [10 6] [14 9] [8 8] [11 12] [8 9] [10 12] [8 10] [12 6]}
                 :glider  #{[2 0] [2 1] [2 2] [1 2] [0 1]}
                 :hand-circles #{[5 11] [6 12] [8 13] [11 15] [13 16] [14 19] [18 22] [4 11] [5 10] [6 13] [8 14] [11 14] [12 18] [13 19] [14 16] [17 22] [4 12] [8 15] [11 13] [12 19] [15 16] [16 22] [4 13] [8 16] [12 12] [6 8] [9 17] [6 9] [11 18] [10 18] [12 15] [20 21] [22 19] [11 8] [12 8] [15 11] [5 19] [10 8] [13 8] [14 11] [5 18] [7 20] [8 6] [10 5] [11 6] [13 11] [14 8] [11 5] [12 11] [15 8] [9 10] [11 12] [12 4] [14 6] [4 14] [6 16] [9 9] [13 4] [15 6] [4 15] [6 17] [8 10] [10 9] [12 6] [14 4] [4 16] [6 14] [8 11] [13 6] [4 17] [6 15] [8 12] [15 3] [16 3] [17 4] [19 6] [20 6] [21 7] [15 2] [16 4] [18 6] [19 5] [22 9] [17 6] [16 6] [18 4] [21 8] [11 4] [17 8] [18 9] [19 10] [16 8] [19 9] [18 8] [16 11] [17 12] [20 14] [9 21] [20 15] [22 17] [10 21] [17 14] [23 15] [17 13] [19 11] [14 22] [17 16] [18 17] [23 13] [15 22] [16 16] [17 15] [18 18] [19 17] [20 11] [23 12] [12 22] [17 18] [19 16] [20 12] [22 10] [16 18] [20 13]}
                 :space-invader #{[11 11] [12 11] [13 11] [14 11] [15 11] [11 12] [15 12] [11 13] [12 13] [13 13] [14 13] [15 13]}
                 })

(def cells-state (atom #{}))

(def empty-board (vec (for [i (range 0 (* size size))]
                        [i false])))


(defn populate
  "Generate the new board, based on the position of the cells."
  [gen]
  (reduce
   (fn [b c]
     (let [[y x]  c]
       (if (and (< -1 y size) (< -1 x size))
         (assoc-in b [(->loc c) 1] true)
         b)))
   empty-board gen))


(<= 0 23 48)

(defn make-step!
  "Swap! the world with the new state."
  []
  (swap! cells-state step))



(defn capturing?
  "Indicate if capturing mode is on/off"
  [owner]
  (om/get-state owner [:capture]))

(defn capture!
  [owner id]
  (om/update-state! owner [:captured] #(conj % id)))

(defn wipe-board!
  "Kill living cells and new ones"
  [app owner]
  (om/update! app #{})
  (om/set-state! owner :captured #{})
  (om/set-state! owner :capture false))

;;;;;;;;;;;;;;; Events handlers ;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti board-event (fn [[e _] app owner] e))


(defmethod board-event :hover
  [[_ id] app owner]
  (when (capturing? owner)
    (capture! owner id)))


(defmethod board-event :click
  [[_ id] app owner]
  (capture! owner id)
  (when (capturing? owner)
    (let [new-cells (map ->cell (om/get-state owner [:captured]))]
      (om/transact! app #(set (concat % new-cells))))
    (om/set-state! owner [:captured] #{}))
  (om/update-state! owner [:capture] not))


(defmethod board-event :key
  [[_ key] app owner]
  (condp = key
    ESCAPE_KEY (if (capturing? owner) (om/set-state! owner [:capture] false))
    SPACE_BAR (wipe-board! app owner)
    ENTER (board-event [:start nil] app owner)
    BACK (board-event [:stop nil] app owner)
    (prn key)))


(defmethod board-event :select
  [[_ key] app owner]
  (let [predef (om/get-shared owner :predef)
        new-cells ((keyword key) predef)]
    (om/transact! app #(set (concat % new-cells)))))


(defmethod board-event :start
  [[_ _] app owner]
  (when-not (om/get-state owner :timer-id)
   (let [delay (om/get-state owner :delay)
         timer-id (js/setInterval make-step! delay)]
    (om/set-state! owner :timer-id timer-id ))))

(defmethod board-event :stop
  [[_ _] app owner]
  (let [timer-id (om/get-state owner :timer-id)]
    (js/clearInterval timer-id)
    (om/set-state! owner :timer-id nil )))

;;;;;;;;;;;;;; Om components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn cell-view
  "Display of a single cell."
  [[id s] owner]
  (reify
   om/IRenderState
    (render-state [_ {:as state :keys [chan captured]}]
                  (let [style (when s "live")
                        style (if (contains? captured id) (str style " captured" ) style)]
                    (dom/div #js {:className style
                                  :id id
                                  :onMouseOver #(put! chan [:hover id])
                                  :onClick #(put! chan [:click id])} "")))))
(defn board-view
  "Display of the entire board."
  [app owner]
  (reify
    om/IRenderState
    (render-state [_ {:as state :keys [chan]}]
                  (apply dom/div #js {:id "visualizer"
                                      :tabIndex 1
                                      :onKeyDown #(put! chan  [:key (.-which  %)])}
                         (om/build-all cell-view app {:state state})))))
(defn selection-view
  [app owner]
 (reify
   om/IRenderState
   (render-state [_ {:as state :keys [chan]}]
                 (let [predef (om/get-shared owner :predef)]
                   (apply dom/select #js {:className "predef"
                                          :onChange (fn [e] (put! chan [:select (-> e .-target .-value)]))}
                          (map #(dom/option #js {:value (name %)} (name %)) (keys predef)))))) )

(defn start-button
  [app owner]
  (reify
    om/IRenderState
    (render-state [_ {:as state :keys [chan]}]
                  (dom/div nil
                           (dom/button #js {:className "stop"
                                            :onClick #(put! chan [:stop nil])} "Stop" )
                           (dom/button #js {:className "start"
                                            :onClick #(put! chan [:start nil])} "Start" )))))


(defn app-view
  "Display of the entire board."
  [app owner]
  (reify
    om/IInitState
    (init-state [_]
                {:chan (chan)
                 :capture false
                 :captured #{}
                 :timer-id nil
                 :delay 270})
    om/IWillMount
    (will-mount [_]
                (let [chan (om/get-state owner [:chan])]
                 (go (loop []
                       (let [[e id] (<! chan)]
                         (board-event [e id] app owner ))
                       (recur)))))
    om/IRenderState
    (render-state [_ state]
                  (dom/div nil
                           (om/build start-button app {:state state})
                           (om/build selection-view app {:state state})
                           (dom/button #js {:onClick #(om/update! app #{})
                                            :className "kill"} "wipe board !")
                           (om/build board-view app {:fn populate
                                                     :state state})))))


(om/root
 app-view
 cells-state
 {:target (. js/document (getElementById "board"))
  :shared {:predef predefined}})
