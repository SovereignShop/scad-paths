(ns scad-paths.core
  (:require
   [scad-paths.utils :as u]
   [scad-clj.model :as m]))

(defmulti path-segment (fn [op pose segments args] op))

(defn path [args & path]
  (let [curr-args (into {:or 10 :curve-radius 7 :shell 1/2 :fn 100 :shape (u/circle-shell 3 2)}
                        args)]
    (loop [pose [0 0 0]
           [[op & opts] & steps] path
           args curr-args
           ret []]
      (if (nil? op)
        (m/union ret)
        (let [options (into args (apply hash-map opts))
              [new-pose parts] (path-segment op pose ret options)]
          (recur new-pose steps options parts))))))

(defmethod path-segment ::left
  [_ [x y angle] segments {:keys [curve-radius fn shape gap]}]
  (let [part (binding [m/*fn* fn]
               (->> shape
                    (m/translate [curve-radius 0 0])
                    (m/extrude-rotate {:angle 90})
                    (m/translate [(- curve-radius) 0 0])
                    (m/rotatec [0 0 (- angle)])
                    (m/translate [x y 0])))
        new-angle (- angle (/ u/pi 2))]
    [[(+ x (* curve-radius (- (- (Math/cos angle) (Math/cos new-angle)))))
      (+ y (* curve-radius (- (- (Math/sin new-angle) (Math/sin angle)))))
      new-angle]
     (if gap
       segments
       (conj segments part))]))

(defmethod path-segment ::right
  [_ [x y angle] segments {:keys [curve-radius fn shape gap]}]
  (let [part (binding [m/*fn* fn]
               (->> shape
                    (m/translate [curve-radius 0 0])
                    (m/extrude-rotate {:angle 90})
                    (m/translate [(- curve-radius) 0 0])
                    (m/rotatec [0 u/pi 0])
                    (m/rotatec [0 0 (- angle)])
                    (m/translate [x y 0])))
        new-angle (+ angle (/ u/pi 2))]
    [[(+ x (* curve-radius (- (Math/cos angle) (Math/cos new-angle))))
      (+ y (* curve-radius (- (Math/sin new-angle) (Math/sin angle))))
      new-angle]
     (if gap
       segments
       (conj segments part))]))

(defmethod path-segment ::curve
  [_ [x y angle] segments {:keys [curve-radius curve-angle fn shape gap]}]
  (let [part (binding [m/*fn* fn]
               (if (pos? curve-angle)
                 (->> shape
                      (m/translate [curve-radius 0 0])
                      (m/extrude-rotate {:angle curve-angle})
                      (m/translate [(- curve-radius) 0 0])
                      (m/rotatec [0 u/pi 0])
                      (m/rotatec [0 0 (- angle)])
                      (m/translate [x y 0]))
                 (->> shape
                      (m/translate [curve-radius 0 0])
                      (m/extrude-rotate {:angle (Math/abs curve-angle)})
                      (m/translate [(- curve-radius) 0 0])
                      (m/rotatec [0 0 (- angle)])
                      (m/translate [x y 0]))))
        new-angle (+ angle (* 0.01745329 curve-angle))]
    [[(+ x (* curve-radius ((if (pos? curve-angle) + -)
                            (- (Math/cos angle) (Math/cos new-angle)))))
      (+ y (* curve-radius ((if (pos? curve-angle) + -)
                            (- (Math/sin new-angle) (Math/sin angle)))))
      new-angle]
     (if gap
       segments
       (conj segments part))]))

(defmethod path-segment ::forward
  [_ [x y angle] segments {:keys [fn length shape gap] :or {length 10}}]
  (let [part (binding [m/*fn* fn]
               (->> shape
                    (m/extrude-linear {:height length :center false})
                    (m/rotatec [(- (u/half u/pi)) 0 0])
                    (m/rotatec [0 0 (- angle)])
                    (m/translate [x y 0])))]
    [[(+ x (* length (Math/sin angle)))
      (+ y (* length (Math/cos angle)))
      angle]
     (if gap
       segments
       (conj segments part))]))

(defmethod path-segment ::hull
  [_ [x y angle] segments {:keys [fn]}]
  (let [part (binding [m/*fn* fn]
               (m/hull (-> segments pop peek)
                       (peek segments)))]
    [[x y angle]
     (conj (-> segments pop pop) part)]))

(defn left [& opts]
  `(::left ~@opts))

(defn right [& opts]
  `(::right ~@opts))

(defn curve [& opts]
  `(::curve ~@opts))

(defn forward [& opts]
  `(::forward ~@opts))

(defn hull [& opts]
  `(::hull ~@opts))
