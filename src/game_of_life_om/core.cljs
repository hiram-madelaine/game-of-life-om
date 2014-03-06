(ns game-of-life-om.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

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


(def cells-state (atom #{[2 0] [2 1] [2 2] [1 2] [0 1]}))



(defn ->loc
  "Give the absolute position in one dimension."
  [[y x]]
  [(+ x (* size y))])


(def empty-board (vec (repeat (* size size) nil)))



(defn populate
  "Generate the new board, based on the position of the cells."
  [gen]
  (reduce
   (fn [b c] (assoc-in b (->loc c) :x))
   empty-board gen))

(def board (atom (populate @cells-state)))


(defn make-step!
  "Swpa! the world with the new state."
  []
  (swap! cells-state step)
  (reset! board (populate @cells-state)))



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
 board
 {:target (. js/document (getElementById "board"))})

;;; Animate !

(js/setInterval make-step! 500)


(defn animate []
  (.requestAnimationFrame js/window animate)
  (make-step!))

#_(animate)
