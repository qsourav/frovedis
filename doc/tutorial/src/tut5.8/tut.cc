#include <frovedis.hpp>
#include <frovedis/dataframe.hpp>

struct multiply {
  double operator()(int a, double b){return a * b;}
  SERIALIZE_NONE
};

int main(int argc, char* argv[]){
  frovedis::use_frovedis use(argc, argv);

  auto t =
    frovedis::make_dftable_loadtext("./t.csv", 
                                    {"int", "double"},
                                    {"c1", "c2"});

  std::cout << "num_row: " << t.num_row() << std::endl;
  std::cout << "num_col: " << t.num_col() << std::endl;
  std::cout << "columns: ";
  for(auto &i: t.columns()) std::cout << i << " ";
  std::cout << std::endl;
  std::cout << "dtypes: ";
  for(auto &i: t.dtypes()) std::cout << i.first << ":" << i.second << " ";
  std::cout << std::endl;
  std::cout << std::endl;
  
  std::cout << "count: " << t.count("c1") << std::endl;
  std::cout << "sum: " << t.sum<double>("c2") << std::endl;
  std::cout << "max: " << t.max<double>("c2") << std::endl;
  std::cout << "min: " << t.min<double>("c2") << std::endl;
  std::cout << "avg: " << t.avg("c2") << std::endl;
  std::cout << std::endl;

  t.calc<double, int, double>("multiply", multiply(), "c1", "c2");
  t.call_function(frovedis::add_col_as("c1","c2","add"));
  t.show();
}
