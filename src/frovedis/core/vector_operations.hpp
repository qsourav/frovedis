#ifndef _VEC_OPERATIONS_ 
#define _VEC_OPERATIONS_

#include <limits>
#include <cmath>
#include "utility.hpp"
#include "exceptions.hpp"
#include "set_operations.hpp"
#include "mpihelper.hpp"

#define NOVEC_LEN 5

/*
 *  This header contains frequently used vector operations in ML algorithms
 *  similar to following numpy operations on 1D array
 *    numpy.sum(x) -> vector_sum(x)
 *    numpy.square(x) -> vector_square(x)
 *    numpy.mean(x) -> vector_mean(x)
 *    numpy.sort(x) -> vector_sort(x)
 *    numpy.count_nonzero(x) -> vector_count_nonzero(x)
 *      additionally available: vector_count_positives(x) and vector_count_negatives(x)
 *    numpy.zeros(sz) -> vector_zeros(sz)
 *    numpy.ones(sz) -> vector_ones(sz)
 *    numpy.full(sz, val) [numpy.ndarray.fill(val)] -> vector_full(sz, val)
 *    numpy.ndarray.astype(dtype) -> vector_astype<T>()
 *    numpy.arange(st, end, step) -> vector_arrange(st, end, step)
 *    numpy.unique(x, ...) -> vector_unique(x, ...)
 *    numpy.bincount(x) -> vector_bincount(x) (x should be non-negative int-vector)
 *    numpy.divide(x, y) -> vector_divide(x, y) or x / y
 *    numpy.multiply(x, y) -> vector_multiply(x, y) or x * y
 *    numpy.add(x, y) -> vector_add(x, y) or x + y
 *    numpy.subtract(x, y) -> vector_subtract(x, y) or x - y
 *    numpy.negative(x) -> vector_negative(x) or -x
 *    numpy.dot(x, y) or blas.dot(x, y) -> vector_dot(x, y) 
 *    numpy.dot(x, x) or numpy.sum(numpy_square(x)) -> vector_squared_sum(x)
 *    numpy.sum(numpy_square(x - y)) -> vector_ssd(x, y) [sum squared difference]
 *    numpy.sum(numpy_square(x - numpy.mean(x)) -> vector_ssmd(x) [sum squared mean difference]
 *    numpy.sum(x * scalar) -> vector_scaled_sum(x, scala)
 *    numpy.sum(x / scalar) -> vector_scaled_sum(x, 1 / scala)
 *    blas.axpy(x, y, alpha) -> vector_axpy(x, y, alpha) [returns alpga * x + y]
 *    numpy.log(x) -> vector_log(x)
 *    numpy.negative(numpy.log(x)) or -numpy.log(x) -> vector_negative_log(x) 
 *    numpy.argmax(x) -> vector_argmax(x)
 *    numpy.argmin(x) -> vector_argmin(x)
 *    numpy.amax(x) -> vector_amax(x)
 *    numpy.amin(x) -> vector_amin(x)
 *    numpy.clip(x, min, max) -> vector_clip(x, min, max)
 *    numpy.take(x, idx) -> vector_take(x, idx)
 *    sklearn.preprocessing.binarize(x, thr) -> vector_binarize(x, thr)
 *
 *  Additionally contains:
 *    debug_print_vector(x, n) - to print fist n and last n elements in vector x
 *    do_allgather(x) - returns gathered vector from all process (must be called by all process from worker side)
 *
 */

namespace frovedis {

// similar to numpy.bincount()
std::vector<size_t>
vector_bincount(const std::vector<int>& vec); // defined in vector_operations.cc

// * if limit = 0, it prints all elements in the input vector.
// * if limit = x and size of vector is more than twice of x, 
// then it prints first "x" and last "x" elements in the input vector.
// * if size of vector is less than twice of x, then it prints all elements.
template <class T>
void debug_print_vector(const std::vector<T>& vec,
                        size_t limit = 0) {
  if (limit == 0 || vec.size() < 2*limit) {
    for(auto& i: vec){ std::cout << i << " "; }
    std::cout << std::endl;
  }
  else {
    for(size_t i = 0; i < limit; ++i) std::cout << vec[i] << " ";
    std::cout << " ... ";
    auto size = vec.size();
    for(size_t i = size - limit; i < size; ++i) std::cout << vec[i] << " ";
    std::cout << std::endl;
  }
}

// must be called from local process (worker)
template <class T>
std::vector<T> do_allgather(std::vector<T>& vec) {
  int size = vec.size();
  auto nproc = get_nodesize();
  std::vector<int> sizes(nproc); auto sizesp = sizes.data();
  std::vector<int> displ(nproc); auto displp = displ.data();
  typed_allgather(&size, 1, sizesp, 1, frovedis_comm_rpc);
  int tot_size = 0; for(int i = 0; i < nproc; ++i) tot_size += sizesp[i];
  displp[0] = 0;
#pragma _NEC novector
  for(int i = 1; i < nproc; ++i) displp[i] = displ[i-1] + sizesp[i-1];
  std::vector<T> gathered_vec(tot_size);
  typed_allgatherv(vec.data(), size,
                   gathered_vec.data(), sizesp, displp,
                   frovedis_comm_rpc);
  //std::cout << "[rank " << get_selfid() << "]: vec: "; debug_print_vector(vec);
  //std::cout << "[rank " << get_selfid() << "]: recvcounts: "; debug_print_vector(sizes);
  //std::cout << "[rank " << get_selfid() << "]: displacements: "; debug_print_vector(displ);
  //std::cout << "[rank " << get_selfid() << "]: gathered: "; debug_print_vector(gathered_vec);
  return gathered_vec;
}

// similar to numpy.ndarray.astype()
template <class R, class T>
std::vector<R>
vector_astype(const std::vector<T>& vec) {
  auto vecsz = vec.size();
  std::vector<R> ret(vecsz);
  auto vptr = vec.data();
  auto rptr = ret.data();
  for(size_t i = 0; i < vecsz; ++i) rptr[i] = static_cast<R>(vptr[i]);
  return ret;
}

// similar to numpy.sum(x)
template <class T>
T vector_sum(const std::vector<T>& vec) {
  T sum = 0;
  auto vecsz = vec.size();
  auto vecp = vec.data();
  for(size_t i = 0; i < vecsz; ++i) sum += vecp[i];
  return sum;
}

// similar to numpy.square(x)
template <class T>
std::vector<T>
vector_square(const std::vector<T>& vec) {
  auto vecsz = vec.size();
  auto vecp = vec.data();
  std::vector<T> ret(vecsz);
  auto retp = ret.data();
  for(size_t i = 0; i < vecsz; ++i) retp[i] = vecp[i] * vecp[i];
  return ret;
}

// similar to numpy.dot(x,x) or numpy.sum(numpy.square(x))
template <class T>
T vector_squared_sum(const std::vector<T>& vec) {
  auto sz = vec.size();
  if (sz == 0) return static_cast<T>(0);
  auto vptr = vec.data();
  // overflow handling
  auto maxval = std::abs(vptr[0]);
  T zero = static_cast<T>(0);
  for(size_t i = 0; i < sz; ++i) {
    auto absval = vptr[i] * ((vptr[i] >= zero) - (vptr[i] < zero));
    if (absval > maxval) maxval = absval;
  }
  auto one_by_max = static_cast<T>(1.0) / maxval;
  T sqsum = 0.0;
  for(size_t i = 0; i < sz; ++i) {
    auto tmp = vptr[i] * one_by_max; // dividing with max to avoid overflow!
    sqsum += tmp * tmp;
  }
  return sqsum * maxval * maxval;
}

// similar to numpy.mean(x)
template <>
int vector_squared_sum(const std::vector<int>& vec); // defined in vector_operations.cc

template <class T>
double vector_mean(const std::vector<T>& vec) {
  return static_cast<double>(vector_sum(vec)) / vec.size();
}

// sum squared difference: similar to numpy.sum(numpy.square(x - y)) or numpy.dot(x - y, x - y)
template <class T>
T vector_ssd(const std::vector<T>& v1,
             const std::vector<T>& v2) {
  auto size = v1.size();
  checkAssumption(size == v2.size());
/*
  auto v1p = v1.data();
  auto v2p = v2.data();
  T sq_error = 0;
  for(size_t i = 0; i < size; ++i) {
    auto error = v1p[i] - v2p[i];
    sq_error += (error * error); // might overflow here...
  }
  return sq_error;
*/
  return vector_squared_sum(v1 - v2); // handles overflow for non-integer vector
}

// sum squared mean difference: similar to numpy.sum(numpy.square(x - numpy.mean(x)))
template <class T>
double vector_ssmd(const std::vector<T>& vec) {
  auto size = vec.size();
  auto vptr = vec.data();
  auto mean = vector_mean(vec);
/*
  double sq_mean_error = 0.0;
  for(size_t i = 0; i < size; ++i) {
    auto error = vptr[i] - mean;
    sq_mean_error += (error * error); // might overflow here...
  }
  return sq_mean_error;
*/
  std::vector<double> error(size); auto eptr = error.data();
  for(size_t i = 0; i < size; ++i) eptr[i] = vptr[i] - mean; 
  return vector_squared_sum(error); // handles overflow for non-integer vector
}

// TODO: support decremental case 10 to 2 etc., negative case -10 to -2 egtc.
// similar to numpy.arange(st, end, step)
template <class T>
std::vector<T>
vector_arrange(const T& st,
               const T& end,
               const T& step = 1) {
  checkAssumption(step != 0);
  if (st >= end && step > 0) return std::vector<T>(); // quick return
  auto sz = ceil_div(end - st, step);
  std::vector<T> ret(sz);
  auto retp = ret.data();
  for(size_t i = 0; i < sz; i += step) retp[i] = st + i;
  return ret;
}

template <class T>
std::vector<T>
vector_arrange(const size_t& end) {
  return vector_arrange<T>(0, end);
}

// similar to numpy.sort(x)
template <class T>
std::vector<T> 
vector_sort(const std::vector<T>& vec,
            bool positive_only = false) {
  auto copy_vec = vec; // copying, since radix_sort operates inplace
  radix_sort(copy_vec, positive_only);
  return copy_vec;
}

template <class T>
std::vector<T> 
vector_sort(const std::vector<T>& vec,
            std::vector<size_t>& pos,
            bool positive_only = false) {
  auto copy_vec = vec; // copying, since radix_sort operates inplace
  pos = vector_arrange<size_t>(vec.size());
  radix_sort(copy_vec, pos, positive_only);
  return copy_vec;
}

// TODO: add vector_count(with condition as function pointer)
// similar to numpy.count_nonzero()
template <class T>
size_t vector_count_nonzero(const std::vector<T>& vec) {
  size_t count = 0;
  auto size = vec.size();
  auto vptr = vec.data();
  for(size_t i = 0; i < size; ++i) count += !vptr[i];
  return size - count;
}

template <class T>
size_t vector_count_positives(const std::vector<T>& vec) {
  auto vecsz = vec.size();
  auto vptr = vec.data();
  size_t count = 0;
  for(size_t i = 0; i < vecsz; ++i) count += vptr[i] > 0;
  return count;
}

template <class T>
size_t vector_count_negatives(const std::vector<T>& vec) {
  auto vecsz = vec.size();
  auto vptr = vec.data();
  size_t count = 0;
  for(size_t i = 0; i < vecsz; ++i) count += vptr[i] < 0;
  return count;
}

// similar to numpy.zeros()
template <class T>
std::vector<T> 
vector_zeros(const size_t& size) {
  return std::vector<T>(size); // default initialization of std::vector is with zero
}

// similar to numpy.ones()
template <class T>
std::vector<T> 
vector_ones(const size_t& size) {
  std::vector<T> ret (size);
  auto rptr = ret.data();
  for(size_t i = 0; i < size; ++i) rptr[i] = static_cast<T>(1);
  return ret;
}

// similar to numpy.full()
template <class T>
std::vector<T> 
vector_full(const size_t& size, const T& val) {
  std::vector<T> ret (size);
  auto rptr = ret.data();
  for(size_t i = 0; i < size; ++i) rptr[i] = val;
  return ret;
}

// similar to numpy.unique()
template <class T, class I = size_t>
std::vector<T>
vector_unique(const std::vector<T>& vec,
              std::vector<size_t>& unique_indices,
              std::vector<I>& unique_inverse,
              std::vector<size_t>& unique_counts,
              std::vector<I>& inverse_target,
              bool positive_only = false) {
  auto vecsz = vec.size();
  if (vecsz == 0) return std::vector<T>(); // quick return
  std::vector<size_t> indices;
  auto sorted = vector_sort(vec, indices, positive_only);
  auto sep = set_separate(sorted);
  auto count = sep.size() - 1;
  std::vector<T> unique(count);
  unique_indices.resize(count);
  unique_counts.resize(count);
  unique_inverse.resize(vecsz);
  auto sepvalp = sep.data();
  auto unqvalp = unique.data();
  auto unqindp = unique_indices.data();
  auto unqcntp = unique_counts.data();
  auto unqinvp = unique_inverse.data();
  auto vecp = sorted.data();
  auto indp = indices.data();
  for(size_t i = 0; i < count; ++i) {
    unqvalp[i] = vecp[sepvalp[i]];
    unqindp[i] = indp[sepvalp[i]];
    unqcntp[i] = sepvalp[i + 1] - sepvalp[i];
  }
  if(inverse_target.size() == 0) 
    inverse_target = vector_arrange<I>(count); // for zero-based encoding
  auto tptr = inverse_target.data();
  require(inverse_target.size() == count, 
  std::string("vector_unique: size of inverse_target differs with no. of ") +
  std::string("unique labels in input vector!\n"));

  for(size_t i = 0; i < count; ++i) {
    // expanded till 10 to avoid performance issue with tiny vector loop length
    if (unqcntp[i] == 1) {
      unqinvp[indp[sepvalp[i]]] = tptr[i];
    }
    else if (unqcntp[i] == 2) {
      unqinvp[indp[sepvalp[i]]] = tptr[i];
      unqinvp[indp[sepvalp[i] + 1]] = tptr[i];
    }
    else if (unqcntp[i] == 3) {
      unqinvp[indp[sepvalp[i]]] = tptr[i];
      unqinvp[indp[sepvalp[i] + 1]] = tptr[i];
      unqinvp[indp[sepvalp[i] + 2]] = tptr[i];
    }
    else if (unqcntp[i] == 4) {
      unqinvp[indp[sepvalp[i]]] = tptr[i];
      unqinvp[indp[sepvalp[i] + 1]] = tptr[i];
      unqinvp[indp[sepvalp[i] + 2]] = tptr[i];
      unqinvp[indp[sepvalp[i] + 3]] = tptr[i];
    }
    else if (unqcntp[i] == 5) {
      unqinvp[indp[sepvalp[i]]] = tptr[i];
      unqinvp[indp[sepvalp[i] + 1]] = tptr[i];
      unqinvp[indp[sepvalp[i] + 2]] = tptr[i];
      unqinvp[indp[sepvalp[i] + 3]] = tptr[i];
      unqinvp[indp[sepvalp[i] + 4]] = tptr[i];
    }
    else if (unqcntp[i] == 6) {
      unqinvp[indp[sepvalp[i]]] = tptr[i];
      unqinvp[indp[sepvalp[i] + 1]] = tptr[i];
      unqinvp[indp[sepvalp[i] + 2]] = tptr[i];
      unqinvp[indp[sepvalp[i] + 3]] = tptr[i];
      unqinvp[indp[sepvalp[i] + 4]] = tptr[i];
      unqinvp[indp[sepvalp[i] + 5]] = tptr[i];
    }
    else if (unqcntp[i] == 7) {
      unqinvp[indp[sepvalp[i]]] = tptr[i];
      unqinvp[indp[sepvalp[i] + 1]] = tptr[i];
      unqinvp[indp[sepvalp[i] + 2]] = tptr[i];
      unqinvp[indp[sepvalp[i] + 3]] = tptr[i];
      unqinvp[indp[sepvalp[i] + 4]] = tptr[i];
      unqinvp[indp[sepvalp[i] + 5]] = tptr[i];
      unqinvp[indp[sepvalp[i] + 6]] = tptr[i];
    }
    else if (unqcntp[i] == 8) {
      unqinvp[indp[sepvalp[i]]] = tptr[i];
      unqinvp[indp[sepvalp[i] + 1]] = tptr[i];
      unqinvp[indp[sepvalp[i] + 2]] = tptr[i];
      unqinvp[indp[sepvalp[i] + 3]] = tptr[i];
      unqinvp[indp[sepvalp[i] + 4]] = tptr[i];
      unqinvp[indp[sepvalp[i] + 5]] = tptr[i];
      unqinvp[indp[sepvalp[i] + 6]] = tptr[i];
      unqinvp[indp[sepvalp[i] + 7]] = tptr[i];
    }
    else if (unqcntp[i] == 9) {
      unqinvp[indp[sepvalp[i]]] = tptr[i];
      unqinvp[indp[sepvalp[i] + 1]] = tptr[i];
      unqinvp[indp[sepvalp[i] + 2]] = tptr[i];
      unqinvp[indp[sepvalp[i] + 3]] = tptr[i];
      unqinvp[indp[sepvalp[i] + 4]] = tptr[i];
      unqinvp[indp[sepvalp[i] + 5]] = tptr[i];
      unqinvp[indp[sepvalp[i] + 6]] = tptr[i];
      unqinvp[indp[sepvalp[i] + 7]] = tptr[i];
      unqinvp[indp[sepvalp[i] + 8]] = tptr[i];
    }
    else if (unqcntp[i] == 10) {
      unqinvp[indp[sepvalp[i]]] = tptr[i];
      unqinvp[indp[sepvalp[i] + 1]] = tptr[i];
      unqinvp[indp[sepvalp[i] + 2]] = tptr[i];
      unqinvp[indp[sepvalp[i] + 3]] = tptr[i];
      unqinvp[indp[sepvalp[i] + 4]] = tptr[i];
      unqinvp[indp[sepvalp[i] + 5]] = tptr[i];
      unqinvp[indp[sepvalp[i] + 6]] = tptr[i];
      unqinvp[indp[sepvalp[i] + 7]] = tptr[i];
      unqinvp[indp[sepvalp[i] + 8]] = tptr[i];
      unqinvp[indp[sepvalp[i] + 9]] = tptr[i];
    }
    else {
      for(size_t j = sepvalp[i]; j < sepvalp[i + 1]; ++j) 
        unqinvp[indp[j]] = tptr[i];
    } 
  }
  return unique;
}

template <class T, class I = size_t>
std::vector<T>
vector_unique(const std::vector<T>& vec,
              std::vector<size_t>& unique_indices,
              std::vector<I>& unique_inverse,
              std::vector<size_t>& unique_counts,
              bool positive_only = false) {
  std::vector<I> inverse_target;
  return vector_unique(vec, unique_indices, 
                       unique_inverse, unique_counts, 
                       inverse_target, positive_only);
}

template <class T>
std::vector<T>
vector_unique(const std::vector<T>& vec,
              bool positive_only = false) {
  return set_unique(vector_sort(vec, positive_only));
}

// similar to numpy.log()
template <class T>
std::vector<double>
vector_log(const std::vector<T>& vec) {
  auto vecsz = vec.size();
  std::vector<double> ret(vecsz);
  auto vecp = vec.data();
  auto retp = ret.data();
  for(size_t i = 0; i < vecsz; ++i) retp[i] = std::log(vecp[i]);
  return ret;
}

// similar to numpy.divide()
template <class T>
std::vector<T>
vector_divide(const std::vector<T>& v1,
              const std::vector<T>& v2) {
  auto vecsz = v1.size();
  checkAssumption(vecsz == v2.size());
  std::vector<T> ret(vecsz);
  auto v1p = v1.data();
  auto v2p = v2.data();
  auto retp = ret.data();
  int divzero = 0;
  for(size_t i = 0; i < vecsz; ++i) {
    if (v2p[i] == 0) { 
      divzero = 1;
      retp[i] = 0;
    }
    else retp[i] = v1p[i] / v2p[i];
  }
  if(divzero) REPORT_WARNING(WARNING_MESSAGE, 
  "RuntimeWarning: divide by zero encountered in divide");
  return ret;
}

template <class T>
std::vector<T>
vector_divide(const std::vector<T>& vec,
              const T& by_elem) {
  auto vecsz = vec.size();
  if (by_elem == 0) {
    REPORT_WARNING(WARNING_MESSAGE,
        "RuntimeWarning: divide by zero encountered in divide");
    return vector_zeros<T>(vecsz);
  }
  std::vector<T> ret(vecsz);
  auto vecp = vec.data();
  auto retp = ret.data();
  double one_by_elem = 1.0 / by_elem; 
  for(size_t i = 0; i < vecsz; ++i) retp[i] = vecp[i] * one_by_elem;
  return ret;
}

template <>
std::vector<int>
vector_divide(const std::vector<int>& vec,
              const int& by_elem); // defined in vector_operations.cc

template <class T>
std::vector<T>
operator/ (const std::vector<T>& vec,
           const T& by_elem) {
  return vector_divide(vec, by_elem);
}

template <class T>
std::vector<T>
operator/ (const std::vector<T>& v1,
           const std::vector<T>& v2) {
  return vector_divide(v1, v2);
}

// similar to numpy.multiply()
template <class T>
std::vector<T>
vector_multiply(const std::vector<T>& v1,
                const std::vector<T>& v2) {
  auto vecsz = v1.size();
  checkAssumption(vecsz == v2.size());
  std::vector<T> ret(vecsz);
  auto v1p = v1.data();
  auto v2p = v2.data();
  auto retp = ret.data();
  for(size_t i = 0; i < vecsz; ++i) retp[i] = v1p[i] * v2p[i];
  return ret;
}

template <class T>
std::vector<T>
vector_multiply(const std::vector<T>& v1,
                const T& by_elem) {
  auto vecsz = v1.size();
  std::vector<T> ret(vecsz);
  auto v1p = v1.data();
  auto retp = ret.data();
  for(size_t i = 0; i < vecsz; ++i) retp[i] = v1p[i] * by_elem;
  return ret;
}

template <class T>
std::vector<T>
operator* (const std::vector<T>& v1,
           const std::vector<T>& v2) {
  return vector_multiply(v1, v2);
}

template <class T>
std::vector<T>
operator* (const std::vector<T>& v1,
           const T& by_elem) {
  return vector_multiply(v1, by_elem);
}

// similar to numpy.add()
template <class T>
std::vector<T>
vector_add(const std::vector<T>& v1,
           const std::vector<T>& v2) {
  auto vecsz = v1.size();
  checkAssumption(vecsz == v2.size());
  std::vector<T> ret(vecsz);
  auto v1p = v1.data();
  auto v2p = v2.data();
  auto retp = ret.data();
  for(size_t i = 0; i < vecsz; ++i) retp[i] = v1p[i] + v2p[i];
  return ret;
}

template <class T>
std::vector<T>
vector_add(const std::vector<T>& v1,
           const T& by_elem) {
  auto vecsz = v1.size();
  std::vector<T> ret(vecsz);
  auto v1p = v1.data();
  auto retp = ret.data();
  for(size_t i = 0; i < vecsz; ++i) retp[i] = v1p[i] + by_elem;
  return ret;
}

template <class T>
std::vector<T>
operator+ (const std::vector<T>& v1,
           const std::vector<T>& v2) {
  return vector_add(v1, v2);
}

template <class T>
std::vector<T>
operator+ (const std::vector<T>& v1,
           const T& by_elem) {
  return vector_add(v1, by_elem);
}

// similar to numpy.dot() or blas.dot() - it also supports integer type input vector
template <class T>
T vector_dot(const std::vector<T>& v1,
             const std::vector<T>& v2) {
  auto vecsz = v1.size();
  checkAssumption(vecsz == v2.size());
  auto v1p = v1.data();
  auto v2p = v2.data();
  auto ret = 0;
  for(size_t i = 0; i < vecsz; ++i) ret += v1p[i] * v2p[i];
  return ret;
}

// similar to blas.axpy() - it also supports integer type input vector
template <class T>
std::vector<T>
vector_axpy(const std::vector<T>& v1,
            const std::vector<T>& v2,
            const T& alpha = 1) {
  auto vecsz = v1.size();
  checkAssumption(vecsz == v2.size());
  std::vector<T> ret(vecsz);
  auto v1p = v1.data();
  auto v2p = v2.data();
  auto retp = ret.data();
  for(size_t i = 0; i < vecsz; ++i) retp[i] = alpha * v1p[i] + v2p[i];
  return ret;
}

// similar to numpy.subtract()
template <class T>
std::vector<T>
vector_subtract(const std::vector<T>& v1,
                const std::vector<T>& v2) {
  auto vecsz = v1.size();
  checkAssumption(vecsz == v2.size());
  std::vector<T> ret(vecsz);
  auto v1p = v1.data();
  auto v2p = v2.data();
  auto retp = ret.data();
  for(size_t i = 0; i < vecsz; ++i) retp[i] = v1p[i] - v2p[i];
  return ret;
}

template <class T>
std::vector<T>
vector_subtract(const std::vector<T>& v1,
                const T& by_elem) {
  auto vecsz = v1.size();
  std::vector<T> ret(vecsz);
  auto v1p = v1.data();
  auto retp = ret.data();
  for(size_t i = 0; i < vecsz; ++i) retp[i] = v1p[i] - by_elem;
  return ret;
}

template <class T>
std::vector<T>
operator- (const std::vector<T>& v1,
           const std::vector<T>& v2) {
  return vector_subtract(v1, v2);
}

template <class T>
std::vector<T>
operator- (const std::vector<T>& v1,
           const T& by_elem) {
  return vector_subtract(v1, by_elem);
}

// similar to numpy.negative()
template <class T>
std::vector<T>
vector_negative(const std::vector<T>& vec) {
  auto vecsz = vec.size();
  std::vector<T> ret(vecsz);
  auto vecp = vec.data();
  auto retp = ret.data();
  for(size_t i = 0; i < vecsz; ++i) retp[i] = -vecp[i];
  return ret;
}

// similar to -numpy.log(x) or numpy.negative(numpy.log(x))
template <class T>
std::vector<double>
vector_negative_log(const std::vector<T>& vec) {
  auto vecsz = vec.size();
  std::vector<double> ret(vecsz);
  auto vecp = vec.data();
  auto retp = ret.data();
  for(size_t i = 0; i < vecsz; ++i) retp[i] = -std::log(vecp[i]);
  return ret;
}

template <class T>
std::vector<T>
operator-(const std::vector<T>& vec) {
  return vector_negative(vec);
}

// similar to numpy.sum(vec * al) -> vector_scaled_sum(vec, al)
// For numpy.sum(vec / al) -> numpy.sum(vec * (1 / al)) -> vector_scaled_sum(vec, 1 / al)
// TODO: Fix issue for int-type while doing: vector_scaled_sum(vec, 1 / al)
template <class T>
T vector_scaled_sum(const std::vector<T>& vec,
                    const T& al) {
  auto vecsz = vec.size();
  auto vecp = vec.data();
  T sum = 0;
  for(size_t i = 0; i < vecsz; ++i) sum += vecp[i] * al;
  return sum;
}

// similar to numpy.argmax()
template <class T>
size_t vector_argmax(const std::vector<T>& vec) {
  auto vecsz = vec.size();
  require(vecsz > 0, "vector_argmax: input vector is empty!");
  auto vecp = vec.data();
  size_t maxindx = 0;
  T max = std::numeric_limits<T>::min();
  for(size_t i = 0; i < vecsz; ++i) {
    if (vecp[i] > max) {
      max = vecp[i];
      maxindx = i;
    }
  }
  return maxindx;
}

// similar to numpy.amax()
template <class T>
T vector_amax(const std::vector<T>& vec) {
  return vec[vector_argmax(vec)]; 
}

// similar to numpy.argmin()
template <class T>
size_t vector_argmin(const std::vector<T>& vec) {
  auto vecsz = vec.size();
  require(vecsz > 0, "vector_argmax: input vector is empty!");
  auto vecp = vec.data();
  size_t minindx = 0;
  T min = std::numeric_limits<T>::max();
  for(size_t i = 0; i < vecsz; ++i) {
    if (vecp[i] < min) {
      min = vecp[i];
      minindx = i;
    }
  }
  return minindx;
}

// similar to numpy.amin()
template <class T>
T vector_amin(const std::vector<T>& vec) {
  return vec[vector_argmin(vec)]; 
}

// similar to numpy.clip()
template <class T>
std::vector<T>
vector_clip(const std::vector<T>& vec,
            const T& min = std::numeric_limits<T>::min(),
            const T& max = std::numeric_limits<T>::max()) {
  checkAssumption(min <= max);
  auto vecsz = vec.size();
  std::vector<T> ret(vecsz);
  auto vecp = vec.data();
  auto retp = ret.data();
  for(size_t i = 0; i < vecsz; ++i) {
    if (vecp[i] <= min) retp[i] = min;
    else if (vecp[i] >= max) retp[i] = max;
    else retp[i] = vecp[i]; // within (min, max) range
  }
  return ret;
}

// similar to numpy.take()
template <class T>
std::vector<T>
vector_take(const std::vector<T>& vec,
            const std::vector<size_t>& idx) {
  auto vsz = vec.size();
  require(vsz > 0, "vector_take: input vector is empty!");
  require(idx[vector_argmax(idx)] < vsz, 
  "vector_take: idx contains index which is larger than input vector size!");
  auto sz = idx.size();
  std::vector<T> ret(sz);
  auto vecp = vec.data();
  auto idxp = idx.data();
  auto retp = ret.data();
  for(size_t i = 0; i < sz; ++i) retp[i] = vecp[idxp[i]];
  return ret;
}

// similar to sklearn.preprocessing.binarize()
template <class T>
std::vector<T>
vector_binarize(const std::vector<T>& vec,
                const T& threshold = 0) {
  auto vecsz = vec.size();
  std::vector<T> ret(vecsz);
  auto vecp = vec.data();
  auto retp = ret.data();
  for(size_t i = 0; i < vecsz; ++i) retp[i] = (vecp[i] <= threshold) ? 0 : 1;
  return ret;
}
  
}
#endif
