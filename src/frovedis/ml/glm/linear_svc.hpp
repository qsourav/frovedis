#ifndef __LIN_SVC__
#define __LIN_SVC__

#include <frovedis/ml/metrics.hpp>
#include <frovedis/ml/model_selection/param.hpp>
#include <frovedis/ml/glm/svm_with_sgd.hpp>
#include <frovedis/ml/glm/svm_with_lbfgs.hpp>

namespace frovedis {

template <class T>
struct linear_svm_classifier {
  linear_svm_classifier(int max_iter = 1000,
                        double alpha = 0.01,
                        const std::string& solver = "sgd",
                        double reg_param = 0.01,
                        const std::string& reg_type= "ZERO",
                        bool fit_intercept = false,
                        double mini_batch_fraction = 1.0,
                        int hs = 10,
                        double convergence_tol = 0.001) {
    this->max_iter = max_iter;
    this->alpha = alpha;
    this->solver = solver;
    this->reg_param = reg_param;
    this->reg_type = reg_type;
    this->fit_intercept = fit_intercept;
    this->mbf = mini_batch_fraction;
    this->hist_size = hs;
    this->tol = convergence_tol;
    this->is_fitted = false;
  }
  linear_svm_classifier<T>& 
  set_max_iter(int max_iter) {
    std::string msg = "expected a positive max_iter; received: " + STR(max_iter) + "\n";
    require(max_iter > 0, msg);
    this->max_iter = max_iter;
    return *this;  
  }
  linear_svm_classifier<T>& 
  set_alpha(double alpha) {
    std::string msg = "expected a positive alpha (learning rate); received: " + STR(alpha) + "\n";
    require(alpha > 0, msg);
    this->alpha = alpha;
    return *this;  
  }
  linear_svm_classifier<T>& 
  set_solver(const std::string& solver) {
    std::string msg = "expected sgd or lbfgs; received: " + solver + "\n";
    require(solver == "sgd" || solver == "lbfgs", msg);
    this->solver = solver;
    return *this;  
  }
  linear_svm_classifier<T>& 
  set_reg_param(double reg_param) {
    std::string msg = "expected a positive regularization parameter; received: " + STR(reg_param) + "\n";
    require(reg_param > 0, msg);
    this->reg_param = reg_param;
    return *this;  
  }
  linear_svm_classifier<T>& 
  set_reg_type(const std::string& reg_type) {
    std::string msg = "expected ZERO, L1 or L2; received: " + reg_type + "\n";
    require(reg_type == "ZERO" || reg_type == "L1" || reg_type == "L2", msg);
    this->reg_type = reg_type;
    return *this;  
  }
  linear_svm_classifier<T>& 
  set_intercept(bool is_intercept) {
    this->fit_intercept = is_intercept;
    return *this;  
  }
  linear_svm_classifier<T>& 
  set_mini_batch_fraction(double mbf) {
    std::string msg = "expected a positive mini-batch fraction; received: " + STR(mbf) + "\n";
    require(mbf > 0, msg);
    this->mbf = mbf;
    return *this;  
  }
  linear_svm_classifier<T>& 
  set_hist_size(int hs) {
    std::string msg = "expected a positive history size; received: " + STR(hs) + "\n";
    require(hs > 0, msg);
    this->hist_size = hs;
    return *this;  
  }
  linear_svm_classifier<T>& 
  set_tol(double tol) {
    std::string msg = "expected a positive convergence tolerance; received: " + STR(tol) + "\n";
    require(tol > 0, msg);
    this->tol = tol;
    return *this;  
  }

  // frovedis::grid_sdearch_cv compatible setter
  linear_svm_classifier<T>& 
  set_params(std::vector<std::pair<std::string, param_t>>& data) {
    std::string msg = "";
    for (auto& e: data) {
      auto param = e.first;
      auto val = e.second;
      if(param == "max_iter") {              
        set_max_iter(val.get<int>()); 
        msg += "max_iter: " + val.tt + "; ";
      }
      else if(param == "alpha") { 
        set_alpha(val.get<double>());
        msg += "alpha: " + val.tt + "; ";
      }
      else if(param == "mini_batch_fraction") { 
        set_mini_batch_fraction(val.get<double>());
        msg += "mini batch fraction: " + val.tt + "; ";
      }
      else if(param == "hist_size") {              
        set_hist_size(val.get<int>()); 
        msg += "hist_size: " + val.tt + "; ";
      }
      else if(param == "tol") { 
        set_tol(val.get<double>());
        msg += "tol: " + val.tt + "; ";
      }
      else if(param == "reg_param") { 
        set_reg_param(val.get<double>());
        msg += "reg param: " + val.tt + "; ";
      }
      else if(param == "reg_type") {
        set_reg_type(val.get<std::string>());
        msg += "reg type: " + val.tt + "; ";
      }
      else if(param == "solver") {
        set_solver(val.get<std::string>());
        msg += "solver: " + val.tt + "; ";
      }
      else if(param == "fit_intercept") { 
        set_intercept(val.get<bool>());
        msg += "fit intercept: " + val.tt + "; ";
      }
      else REPORT_ERROR(USER_ERROR, "[linear_svm_classifier] Unknown parameter: '" 
                                     + param + "' is encountered!\n");
    }
    RLOG(DEBUG) << msg << "\n";
    return *this;
  }

  RegType get_regularizer() {
    RegType regularizer = ZERO;
    if (reg_type == "ZERO") regularizer = ZERO;
    else if (reg_type == "L1") regularizer = L1;
    else if (reg_type == "L2") regularizer = L2;
    else REPORT_ERROR(USER_ERROR, "[linear_svm_classifier] Unknown regularizer '" 
                                   + reg_type + "' is encountered!\n");
    return regularizer;
  }

  // MATRIX: can accept both rowmajor and colmajor matrices as for dense data; 
  //         and crs matrix as for sparse data
  template <class MATRIX>
  linear_svm_classifier& 
  fit(MATRIX& mat, dvector<T>& label) {
    if (solver == "sgd") {
      this->model = svm_with_sgd::train(
                      mat, label, max_iter, alpha, mbf,
                      reg_param, get_regularizer(), fit_intercept, tol);
    }
    else if (solver == "lbfgs") {
      this->model = svm_with_lbfgs::train(
                      mat, label, max_iter, alpha, hist_size,
                      reg_param, get_regularizer(), fit_intercept, tol);
    }
    else REPORT_ERROR(USER_ERROR, "Unknown solver is encountered!\n");
    this->is_fitted = true;
    return *this;
  }

  template <class MATRIX>
  std::vector<T> 
  predict(MATRIX& mat) {
    if(!is_fitted) REPORT_ERROR(USER_ERROR, 
                   "[linear_svm_classifier] predict is called before fit\n");
    return mat.data.map(parallel_predict<typename MATRIX::local_mat_type,
                        svm_model<T>, T>,
                        broadcast(model))
                   .template moveto_dvector<T>()
                   .gather();
  }

  template <class MATRIX>
  float score(MATRIX& mat, dvector<T>& label) {
    if(!is_fitted) REPORT_ERROR(USER_ERROR, 
                   "[linear_svm_classifier] score is called before fit\n");
    auto pred_label = predict(mat);
    return accuracy_score(pred_label, label.gather());
  }

  int max_iter, hist_size;
  double alpha, mbf, tol, reg_param;
  std::string reg_type, solver;
  bool fit_intercept;
  svm_model<T> model;
  bool is_fitted;
  SERIALIZE(max_iter, hist_size, alpha, mbf, tol, 
            reg_param, reg_type, solver,
            fit_intercept, model, is_fitted); 
};

}
#endif
