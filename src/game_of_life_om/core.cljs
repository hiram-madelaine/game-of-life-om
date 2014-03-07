(ns game-of-life-om.core
  (:require-macros [dommy.macros :refer [node sel sel1]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [dommy.core :as dommy]))

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
  [(+ x (* size y))])



;;;;;;;;;;;;;;;; States ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def cells-state (atom #{[2 0] [2 1] [2 2] [1 2] [0 1]}))


(def empty-board (vec (repeat (* size size) nil)))


(defn populate
  "Generate the new board, based on the position of the cells."
  [gen]
  (reduce
   (fn [b c] (assoc-in b (->loc c) :x))
   empty-board gen))

(def board (atom (populate @cells-state)))


(defn make-step!
  "Swap! the world with the new state."
  []
  (swap! cells-state step))



;;;;;;;;;;;;;; Om components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn cell-view
  "Display of a single cell."
  [cell owner]
  (om/component
   (let [style (when cell "live")]
     (dom/div #js {:className style} ""))))


(defn board-view
  "Display of the entire board."
  [app owner]
  (om/component
   (apply dom/div #js {:id "visualizer"}
          (om/build-all cell-view app))))

(om/root
 board-view
 cells-state
 {:target (. js/document (getElementById "board"))
  :fn populate})

;;;;;;;;;;; Mark ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn mark!
  "Mark in pink all the divs by direct dom manipulation."
  []
  (doseq [el (sel [:#visualizer :div])]
    (dommy/add-class! el :marker)))

(dommy/listen! (sel1 :button.mark)
                :click mark!)


;;;;;;;;;;;;;;;;;;; Animate ! ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

#_(js/setInterval make-step! 500)





(defn animate []
  (.requestAnimationFrame js/window animate)
  (make-step!))

#_(animate)
