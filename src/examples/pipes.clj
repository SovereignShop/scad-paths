(ns examples.pipes
  (:require
   [scad-clj.scad :as s]
   [scad-clj.model :as m]
   [scad-paths.core
    :refer [model left right forward up down hull path set branch arc defmodel]]))

(->> (path
      [(model :shape (m/circle 6) :mask? false :name :body)
       (model :shape (m/circle 4) :mask? true :name :mask)
       (set :curve-radius 20 :fn 70 :to [:body]) (set :curve-radius 20 :fn 70 :to [:mask])

       (left :angle (/ Math/PI 2) :to [:body])
       (left :angle (/ Math/PI 2) :to [:mask])

       (right :angle (/ Math/PI 2) :to [:body])
       (right :angle (/ Math/PI 2) :to [:mask])

       (forward :length 10 :to [:body])
       (forward :length 10 :to [:mask])

       (up :to [:body])
       (up :to [:mask])])
     (s/write-scad)
     (spit "test.scad"))

;; Or equivalently:

(->> (path [(model :shape (m/circle 6) :mask? false :name :body)
            (model :shape (m/circle 4) :mask? true :name :mask)
            (set :curve-radius 20 :fn 70 :to [:body :mask])

            (left :angle (/ Math/PI 2) :to [:body :mask])
            (right :angle (/ Math/PI 2) :to [:body :mask])
            (forward :length 10 :to [:body :mask])
            (up :to [:body :mask])
            (forward :length 20 :to [:body :mask])])
     (s/write-scad)
     (spit "test.scad"))

;; ... Or equivalently:

(->> (path [(model :shape (m/circle 6) :mask? false :name :body)
            (model :shape (m/circle 4) :mask? true :name :mask)
            (set :curve-radius 20 :fn 70 :to [:body :mask])

            (left :angle (/ Math/PI 2))
            (right :angle (/ Math/PI 2))
            (forward :length 10)
            (up :to [:body :mask])
            (forward :length 20)])
     (s/write-scad)
     (spit "test.scad"))

;; Hulls

(->> (path [(model :shape (m/circle 6) :mask? false :name :body)
            (model :shape (m/circle 4) :mask? true :name :mask)
            (set :curve-radius 20 :fn 70)

            (forward :length 20)

            (set :shape (m/square 20 20) :to [:body])
            (set :shape (m/square 16 16) :to [:mask])

            (forward :length 20)
            (hull)
            (forward :length 20)

            (set :shape (m/circle 6) :to [:body])
            (set :shape (m/circle 4) :to [:mask])

            (forward :length 20)
            (hull)])
     (s/write-scad)
     (spit "test.scad"))

;; Branching

(->> (path [(model :shape (m/circle 6) :mask? false :name :body)
            (model :shape (m/circle 4) :mask? true :name :mask)
            (set :curve-radius 10 :fn 70)

            (branch (left) (right) (forward :length 20))
            (branch (right) (left) (forward :length 20))])
     (s/write-scad)
     (spit "test.scad"))

;; Gaps

(->> (path [(model :shape (m/circle 6) :mask? false :name :body :curve-radius 10 :fn 70)
            (left :angle (/ Math/PI 2) :gap true)
            (right :angle (/ Math/PI 2))
            (left :gap true)
            (right)
            (left :gap true)
            (right)])
     (s/write-scad)
     (spit "test.scad"))

;; Arcs


;; Arcs defined by providing the straight-line distance between the start-point
;; and end-point of the arc, along with the radius.
;;
;; In this example, the distance between Z-intersection points will be 20.
;; You can think of it as a two-point circle.

(defmodel arc-torus
  (model :shape (m/circle 2) :fn 50)
  (arc :side-length 20 :curve-radius 20)
  (arc :side-length 20 :curve-radius 20)
  (arc :side-length 20 :curve-radius 20)
  (arc :side-length 20 :curve-radius 20)
  (arc :side-length 20 :curve-radius 20)
  (arc :side-length 20 :curve-radius 20))

(->> arc-torus
     (s/write-scad)
     (spit "test.scad"))
