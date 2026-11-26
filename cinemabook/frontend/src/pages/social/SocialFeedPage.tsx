import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { ArrowLeft, TrendingUp, Hash } from 'lucide-react';
import { socialService } from '../../services/socialService';
import { PostCard } from '../../components/social/PostCard';
import { CreatePostForm } from '../../components/social/CreatePostForm';
import { useAuthStore } from '../../store/authStore';

export function SocialFeedPage() {
  const { user } = useAuthStore();
  const navigate = useNavigate();
  const [page, setPage] = useState(0);

  const feedKey   = ['socialFeed', page];
  const trendKey  = ['trendingHashtags'];

  const { data: feed, isLoading } = useQuery({
    queryKey: feedKey,
    queryFn: () => socialService.getFeed(page, 20),
  });

  const { data: trending } = useQuery({
    queryKey: trendKey,
    queryFn: () => socialService.getTrendingHashtags(10),
    staleTime: 5 * 60_000,
  });

  return (
    <div className="min-h-screen bg-neutral-950 text-white">
      {/* Header */}
      <header className="border-b border-neutral-800 px-6 py-4 flex items-center justify-between sticky top-0 bg-neutral-950 z-10">
        <div className="flex items-center gap-4">
          <button onClick={() => navigate(-1)}
            className="flex items-center gap-2 text-neutral-400 hover:text-white transition-colors text-sm">
            <ArrowLeft className="w-4 h-4" />
          </button>
          <h1 className="text-xl font-bold">
            Cinema<span className="text-amber-400">Book</span>
            <span className="ml-2 text-sm font-normal text-neutral-400">Social</span>
          </h1>
        </div>
        <Link to="/dashboard" className="text-sm text-neutral-400 hover:text-white transition-colors">
          Browse films →
        </Link>
      </header>

      <main className="max-w-5xl mx-auto px-4 py-6">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">

          {/* Feed column */}
          <div className="md:col-span-2 space-y-4">
            {/* Create post */}
            <CreatePostForm
              queryKeys={[feedKey]}
              placeholder="Share something about cinema… Use #hashtags!"
            />

            {/* Posts */}
            {isLoading ? (
              <div className="space-y-4">
                {Array.from({ length: 5 }).map((_, i) => (
                  <div key={i} className="h-40 bg-neutral-900 border border-neutral-800
                                          rounded-xl animate-pulse" />
                ))}
              </div>
            ) : feed?.posts.length === 0 ? (
              <div className="text-center py-16 bg-neutral-900 border border-neutral-800 rounded-xl">
                <p className="text-neutral-400 mb-2">Your feed is empty.</p>
                <p className="text-neutral-500 text-sm">
                  Follow other users to see their posts here.
                </p>
              </div>
            ) : (
              <>
                {feed?.posts.map(post => (
                  <PostCard key={post.id} post={post} queryKeys={[feedKey]} />
                ))}

                {/* Pagination */}
                {feed && feed.totalPages > 1 && (
                  <div className="flex justify-center gap-3 pt-2">
                    <button onClick={() => setPage(p => Math.max(0, p - 1))}
                      disabled={page === 0}
                      className="px-4 py-2 text-sm bg-neutral-800 rounded-lg
                                 disabled:opacity-40 hover:bg-neutral-700 transition-colors">
                      Previous
                    </button>
                    <span className="text-sm text-neutral-400 self-center">
                      {page + 1} / {feed.totalPages}
                    </span>
                    <button onClick={() => setPage(p => Math.min(feed.totalPages - 1, p + 1))}
                      disabled={page >= feed.totalPages - 1}
                      className="px-4 py-2 text-sm bg-neutral-800 rounded-lg
                                 disabled:opacity-40 hover:bg-neutral-700 transition-colors">
                      Next
                    </button>
                  </div>
                )}
              </>
            )}
          </div>

          {/* Sidebar */}
          <div className="space-y-4">

            {/* Trending hashtags */}
            <div className="bg-neutral-900 border border-neutral-800 rounded-xl p-5">
              <h3 className="flex items-center gap-2 font-semibold mb-4 text-sm">
                <TrendingUp className="w-4 h-4 text-amber-400" />
                Trending hashtags
              </h3>
              {trending?.length === 0 ? (
                <p className="text-neutral-500 text-sm">No trending hashtags yet.</p>
              ) : (
                <div className="space-y-2">
                  {trending?.map(h => (
                    <Link key={h.id} to={`/social/hashtag/${h.tag}`}
                      className="flex items-center justify-between group">
                      <span className="flex items-center gap-1.5 text-sm text-blue-400
                                       group-hover:text-blue-300 transition-colors">
                        <Hash className="w-3.5 h-3.5" />
                        {h.tag}
                      </span>
                      <span className="text-xs text-neutral-500">{h.postCount} posts</span>
                    </Link>
                  ))}
                </div>
              )}
            </div>

            {/* My profile quick link */}
            {user && (
              <div className="bg-neutral-900 border border-neutral-800 rounded-xl p-5">
                <div className="flex items-center gap-3 mb-3">
                  <div className="w-10 h-10 rounded-full bg-neutral-700 flex items-center
                                  justify-center font-bold text-sm">
                    {user.displayName?.[0]?.toUpperCase() ?? '?'}
                  </div>
                  <div>
                    <p className="text-sm font-semibold">{user.displayName}</p>
                    <p className="text-xs text-neutral-500">@{user.username}</p>
                  </div>
                </div>
                <Link to={`/profile/${user.username}`}
                  className="block w-full text-center border border-neutral-700
                             hover:border-amber-400 text-sm py-1.5 rounded-lg
                             transition-colors hover:text-amber-400">
                  View my profile
                </Link>
              </div>
            )}
          </div>
        </div>
      </main>
    </div>
  );
}