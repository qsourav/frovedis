F77 = $(MPIF77)
LDADD = $(top_builddir)/PARPACK/SRC/MPI/libparpack$(LIBSUFFIX).la $(LAPACK_LIBS) $(BLAS_LIBS)

SISS = issue46

check_PROGRAMS = $(SISS)

EXTRA_DIST = debug.h stat.h

issue46_SOURCES= issue46.f