#include <frovedis.hpp>

struct n_times {
  n_times(){}
  n_times(int n_) : n(n_) {}
  int operator()(int i){return i*n;}
  int n;
  SERIALIZE(n)
};

int main(int argc, char* argv[]){
  frovedis::use_frovedis use(argc, argv);
  
  std::vector<int> v = {1,2,3,4,5,6,7,8};
  auto d1 = frovedis::make_dvector_scatter(v);
  auto d2 = d1.map<int>(n_times(3));
  auto r = d2.gather();
  for(auto i: r) std::cout << i << std::endl;
}
