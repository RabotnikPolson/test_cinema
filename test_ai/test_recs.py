import sys
import os
sys.path.append(os.getcwd())

import recommender
import main

df, sim = recommender.get_recommendations_model()
try:
    print("Testing franchise recs for 88:")
    res = recommender.get_franchise_recommendations(88, df, 6)
    print(res)
    print("Testing content recs for 88:")
    res = recommender.get_content_recommendations(88, df, sim, 6)
    print(res)
except Exception as e:
    print(f"Error in recommender: {type(e).__name__} - {e}")
