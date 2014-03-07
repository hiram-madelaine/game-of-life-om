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



;;;;;;;;;;;;;; Om components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn cell-view
  "Display of a single cell."
  [[id s] owner]
  (reify
   om/IRenderState
    (render-state [_ {:as state :keys [chan]}]
                  (let [style (when s "live")]
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
                 :capture false})
    om/IWillMount
    (will-mount [_]
                (let [chan (om/get-state owner [:chan])]
                 (go (loop []
                       (let [[e id] (<! chan)]
                         (condp = e
                           :click (om/update-state! owner :capture not)
                           :hover )
                         (when (om/get-state owner [:capture])
                           (capture! app id)))
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

#_(js/setInterval make-step! 200)


#_(prn @cells-state)
#_(reset! cells-state organic)

(defn animate []
  (.requestAnimationFrame js/window animate)
  (make-step!))

#_(animate)
