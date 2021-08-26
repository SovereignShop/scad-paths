(ns scad-paths.core
  (:require
   [scad-paths.utils :as u]
   [scad-clj.model :as m]))

(defmulti path-segment (fn [ctx _ _ _] (:op ctx)))

(defn path [ctx & path]
  (let [curr-context (into {:or 10 :curve-radius 7 :shell 1/2 :fn 100 :shape (u/circle-shell 3 2)}
                           ctx)]
    (loop [pose [0 0 0]
           [[op & args] & steps] path
           ctx curr-context
           ret []]
      (if (nil? op)
        (m/union ret)
        (if (= op ::context)
          (recur pose steps (into ctx (partition-all 2) (vec args)) ret)
          (let [[new-pose parts] (path-segment (assoc ctx :op op) pose ret args)]
            (recur new-pose steps ctx parts)))))))

(defn path-grid [columns ctx & path*]
  (let [paths (apply map vector
                     (map (fn [[l r :as pair]]
                            (if (= :same r)
                              [l l]
                              pair))
                          (partition columns path*)))]
    (mapv #(apply path ctx %) paths)))

(defmethod path-segment ::left
  [{:keys [fn shape gap] :as ctx} [x y angle] segments args]
  (let [[& {:keys [curve-radius]
            :or {curve-radius (:curve-radius ctx)}}] args
        part (binding [m/*fn* fn]
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
  [{:keys [fn shape gap] :as ctx} [x y angle] segments args]
  (let [[& {:keys [curve-radius]
            :or {curve-radius (:curve-radius ctx)}}] args
        part (binding [m/*fn* fn]
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
  [{:keys [fn shape gap] :as ctx} [x y angle] segments args]
  (let [[& {:keys [curve-radius curve-angle]
            :or {curve-radius (:curve-radius ctx)}}] args
        part (binding [m/*fn* fn]
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
  [{:keys [fn shape gap]} [x y angle] segments [& {:keys [length model]}]]
  (let [part (binding [m/*fn* fn]
               (->> (if model
                      model
                      (->> shape
                           (m/extrude-linear {:height length :center false})))
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
  [{:keys [fn]} pose segments _]
  (let [part (binding [m/*fn* fn]
               (m/hull (-> segments pop peek)
                       (peek segments)))]
    [pose
     (conj (-> segments pop pop) part)]))

(defmethod path-segment ::branch
  [ctx [x y angle :as pose] segments args]
  (let [model (apply path ctx args)]
    [pose
     (conj segments
           (->> model
                (m/rotatec [0 0 (- angle)])
                (m/translate [x y 0])))]))

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

(defn branch [& args]
  `(::branch ~@args))

(defn context [& args]
  `(::context ~@args))
