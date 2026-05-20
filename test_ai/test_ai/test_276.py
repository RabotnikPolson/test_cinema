import sys
import os
sys.path.append(os.getcwd())

import recommender
import main

df, sim = recommender.get_recommendations_model()
try:
    print("Testing content recs for 276:")
    res = recommender.get_content_recommendations(276, df, sim, 6)
    print(res)
except Exception as e:
    import traceback
    traceback.print_exc()
