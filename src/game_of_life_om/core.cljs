(ns game-of-life-om.core
  (:require-macros [dommy.macros :refer [node sel sel1]]
                   [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [dommy.core :as dommy]
            [cljs.core.async :refer [chan put! <!]]))

(enable-console-print!)

(def size 32)

;;;;; cgrand solution of game of life


(defn neighbours
  [[x y]]
  (for [dx [-1 0 1] dy [-1 0 1]
        :when (not= 0 dx dy)] [(+ dx x) (+ dy y)]))

(defn step
  "Yields the next state of the world"
  [cells]
  (set
   (for
     [[loc n] (frequencies (mapcat neighbours cells))
      :when (or (= n 3) (and (= n 2) (cells loc)))]
     loc)))

;;;;;;;;;;;;;;; Utils ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ->loc
  "Give the absolute position in one dimension."
  [[y x]]
  (+ x (* size y)))

(defn ->cell
  "Give the cell coord given the absolute position"
  [l]
  ((juxt quot rem) l size))

;;;;;;;;;;;;;;;; States ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def organic #{[14 13] [14 14] [16 13] [16 14] [15 15] [15 12] [13 15]})
(def organic-2 #{[14 13] [14 14] [15 15] [13 15] [15 12] [16 13] [16 14] [17 12]})
(def T->blinker #{[11 12] [12 12] [13 12] [12 13]})
(def blinker #{[12 12] [14 10] [11 6] [14 8] [10 6] [14 9] [8 8] [11 12] [8 9] [10 12] [8 10] [12 6]})
(def glider  #{[2 0] [2 1] [2 2] [1 2] [0 1]})
(def hand-circles #{[5 11] [6 12] [8 13] [11 15] [13 16] [14 19] [18 22] [4 11] [5 10] [6 13] [8 14] [11 14] [12 18] [13 19] [14 16] [17 22] [4 12] [8 15] [11 13] [12 19] [15 16] [16 22] [4 13] [8 16] [12 12] [6 8] [9 17] [6 9] [11 18] [10 18] [12 15] [20 21] [22 19] [11 8] [12 8] [15 11] [5 19] [10 8] [13 8] [14 11] [5 18] [7 20] [8 6] [10 5] [11 6] [13 11] [14 8] [11 5] [12 11] [15 8] [9 10] [11 12] [12 4] [14 6] [4 14] [6 16] [9 9] [13 4] [15 6] [4 15] [6 17] [8 10] [10 9] [12 6] [14 4] [4 16] [6 14] [8 11] [13 6] [4 17] [6 15] [8 12] [15 3] [16 3] [17 4] [19 6] [20 6] [21 7] [15 2] [16 4] [18 6] [19 5] [22 9] [17 6] [16 6] [18 4] [21 8] [11 4] [17 8] [18 9] [19 10] [16 8] [19 9] [18 8] [16 11] [17 12] [20 14] [9 21] [20 15] [22 17] [10 21] [17 14] [23 15] [17 13] [19 11] [14 22] [17 16] [18 17] [23 13] [15 22] [16 16] [17 15] [18 18] [19 17] [20 11] [23 12] [12 22] [17 18] [19 16] [20 12] [22 10] [16 18] [20 13]} )
(def space-invader #{[11 11] [12 11] [13 11] [14 11] [15 11] [11 12] [15 12] [11 13] [12 13] [13 13] [14 13] [15 13]})
(def cells-state (atom #{}))


(def empty-board (vec (for [i (range 0 (* size size))]
                               [i false])))


(defn populate
  "Generate the new board, based on the position of the cells."
  [gen]
  (reduce
   (fn [b c]
     (let [l (->loc c)]
       (if (<= 0 l (dec (* size size)))
         (assoc-in b [l 1] true)
         b)))
   empty-board gen))



(defn make-step!
  "Swap! the world with the new state."
  []
  (swap! cells-state step))


;;;;;;;;;;;;;;; Events handlers ;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti board-event (fn [[e _] app owner] e))


(defmethod board-event :hover
  [[_ id] app owner]
  (when (om/get-state owner [:capture])
    (om/update-state! owner [:captured] #(conj % id))))


(defmethod board-event :click
  [[_ id] app owner]
  (om/update-state! owner [:captured] #(conj % id))
  (when (om/get-state owner [:capture])
    (let [new-cells (map ->cell (om/get-state owner [:captured]))
          _ (prn new-cells)]
      (om/transact! app #(set (concat % new-cells))))
    (om/set-state! owner [:captured] #{}))
  (om/update-state! owner [:capture] not))

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
    (render-state [_ state]
                  (apply dom/div #js {:id "visualizer"}
                         (om/build-all cell-view app {:state state})))))


(defn capture! [app id]
  (om/transact! app (fn [cells] (let [cell (->cell id)]
                                                        (if (contains? cells cell)
                                                          (disj cells cell)
                                                          (conj cells (->cell id)))))))


(defn app-view
  "Display of the entire board."
  [app owner]
  (reify
    om/IInitState
    (init-state [_]
                {:chan (chan)
                 :capture false
                 :captured #{}})
    om/IWillMount
    (will-mount [_]
                (let [chan (om/get-state owner [:chan])]
                 (go (loop []
                       (let [[e id] (<! chan)]
                         (board-event [e id] app owner ))
                       (recur)))))
    om/IRenderState
    (render-state [_ state]
     (om/build board-view app {:fn populate
                               :state state}))))


(om/root
 app-view
 cells-state
 {:target (. js/document (getElementById "board"))})

;;;;;;;;;;; Mark ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn mark!
  "Mark in pink all the divs by direct dom manipulation."
  []
  (doseq [el (sel [:#visualizer :div])]
    (dommy/add-class! el :marker)))

(dommy/listen! (sel1 :button.mark)
                :click mark!)


;;;;;;;;;;;;;;;;;;; Animate ! ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(js/setInterval make-step! 200)


(prn @cells-state)
#_(reset! cells-state hand-circles )

(defn animate []
  (.requestAnimationFrame js/window animate)
  (make-step!))

#_(animate)
