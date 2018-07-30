//
// Each transpose function must have a prototype of the form:
// void trans(int M, int N, int A[N][M], int B[M][N]);
//
// A transpose function is evaluated by counting the number of misses
// on a 1KB direct mapped cache with a block size of 32 bytes.
// Author: Iris Yuan
// (note on style: in trans.c and csim.c, I comment using lower-case letters for readability.)
//
#include <stdio.h>
#include <stdbool.h>

#define N 64
#define M 64

//
// Checks whether B is the transpose of A
int is_transpose(int A[N][M], int B[M][N]);

void transpose2(int A[N][M], int B[M][N]) {
   for (int i = 0; i < N; i ++) {
       for (int j = 0; j < M; j ++) {
            B[j][i] = A[i][j];
       }
   }
}

//
// Transposes A and stores the result in B
void transpose(int A[N][M], int B[M][N]) {

	// illegal to have arrays or more than 12 local variables defined
	// 6 variables are initialized in this function

	int i;
	int j;
	int row;
	int column;
	int diagonal = 0;
	int temp = 0;

    if (N == 64) {
		for (column = 0; column < N; column += 4) {
			for (row = 0; row < N; row += 4) {
			    // iterate over the rows
				for (i = row; i < row + 4; i++) {
					for (j = column; j < column + 4; j++) {
					    // if not a diagonal element
						if (i != j) {
							B[j][i] = A[i][j];
						} else {
							temp = A[i][j];
							diagonal = i;
						}
					}
					if (row == column) {
						B[diagonal][diagonal] = temp;
					}
				}
			}
		}
	}
}

//
// is_transpose - This helper function checks if B is the transpose of
//     A. You can check the correctness of your transpose by calling
//     it before returning from the transpose function.
//
int is_transpose(int A[N][M], int B[M][N])
{
    int i;
    int j;

    for (i = 0; i < N; i++) {
        for (j = 0; j < M; ++j) {
            if (A[i][j] != B[j][i]) {
                return 0;
            }
        }
    }
    return 1;
}

// Execute test cases
int main(int argc, char *argv[]) {

  // int X[N][N][N][N][N];
  //
  // Declare A and B
  int A[N][M];
  int B[M][N];
  //
  // Initialize A
  for(int i = 0; i < N; i++) {
    for(int j = 0; j < M; j++) {
      A[i][j] = i+N*j;
      B[i][j] = A[i][j];
    }
  }

  if(is_transpose(A, B)) {
  		printf("==TRUE:  B is the transpose of A\n");
    } else {
  		printf("==FALSE: B isn't the transpose of A\n");
  }

  //
  transpose(A, B);
  //
  if(is_transpose(A, B)) {
		printf("==TRUE:  B is the transpose of A\n");
  } else {
		printf("==FALSE: B isn't the transpose of A\n");
  }
  //
  return 0;
}

// -- FOOTER --
