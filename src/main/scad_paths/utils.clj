(ns scad-paths.utils
  (:require
   [clojure.core.matrix :as mat]
   [scad-clj.model :as m]))

(def pi Math/PI)

(defn half [x] (/ x 2))

(defn rotation-vector
  "Rodrigues rotation formula"
  [v k a]
  (mat/add
   (mat/mmul (vec v)
             (Math/cos a))
   (mat/mmul (mat/cross k v)
             (Math/sin a))
   (mat/mmul (vec k)
             (mat/dot k v)
             (- 1 (Math/cos a)))))

(defn yaw
  ([v]
   (yaw v (/ pi 2)))
  ([[vx vy vz] a]
   [(rotation-vector vx vz a)
    (rotation-vector vy vz a)
    vz]))

(defn pitch
  ([v]
   (pitch v (/ pi 2)))
  ([[vx vy vz] a]
   [vx
    (rotation-vector vy vx a)
    (rotation-vector vz vx a)]))

(defn roll
  ([v]
   (roll v (/ pi 2)))
  ([[vx vy vz] a]
   [(rotation-vector vx vy a)
    vy
    (rotation-vector vz vy a)]))

(defn go-forward [p v x]
  (mat/add p (mat/mmul (second v) x)))

(defn rotate
  [[vx vy vz] axis a]
  [(rotation-vector vx axis a)
   (rotation-vector vy axis a)
   (rotation-vector vz axis a)])

(defn bAc->a
  "side,angle,side -> side via. law of cosines."
  [b A c]
  (Math/sqrt (- (+ (Math/pow b 2)
                   (Math/pow c 2))
                (* 2 b c (Math/cos A)))))

(defn angle-between [a b]
  (Math/acos (/ (mat/dot a b) (* ( mat/magnitude a) (mat/magnitude b)))))

(defn about-equal? [v1 v2]
  (loop [[x & xs] v1
         [y & ys] v2]
    (cond
      (nil? x) true

      (< (Math/abs (- x y)) 0.00001)
      (recur xs ys)

      :else false)))

(defn opposing? [v1 v2]
  (about-equal? (mat/add v1 v2) [0 0 0]))

(defn- rotation-axis-and-angle [v1 v2 opp]
  (let [cross (mat/cross v1 v2)]
    (if (about-equal? cross [0 0 0])
      (if (opposing? v1 v2)
        [pi opp]
        [0 opp])
      [(angle-between v1 v2) (mat/normalise cross)])))

(def identity-mat [[1 0 0] [0 1 0] [0 0 1]])

(defn ->scad-transform
  "Make an OpenSCAD transformation function between two coordinate frames offset by `pose`."
  ([m pose] (->scad-transform identity-mat m pose))
  ([a b pose]
   (let [[angle ortho] (rotation-axis-and-angle (nth a 0) (nth b 0) [0 0 1])
         c (rotate a ortho angle)
         [angle-2 ortho-2] (rotation-axis-and-angle (nth b 1) (nth c 1) (nth c 0))]
     (comp
      (partial m/translate pose)
      (partial m/rotatev (- angle-2) ortho-2)
      (partial m/rotatev angle ortho)))))
