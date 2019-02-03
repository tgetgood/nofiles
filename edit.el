(defun load-sym-to-temp-buffer (sym)
	(interactive "sSymbol to retrieve: ")
	(let ((buff (generate-new-buffer "temp.clj")))
		(progn
			(switch-to-buffer buff)
			(clojure-mode)

			(cider-interactive-eval (concat "(editor.core/str-v '" sym ")")
															(lambda (s) (insert s)))
			)))
