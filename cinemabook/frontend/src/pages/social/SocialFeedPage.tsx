import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { ArrowLeft, TrendingUp, Hash, Search, Users } from 'lucide-react';
import { socialService } from '../../services/socialService';
import { apiClient } from '../../services/apiClient';
import { PostCard } from '../../components/social/PostCard';
import { CreatePostForm } from '../../components/social/CreatePostForm';
import { useAuthStore } from '../../store/authStore';
import type { ApiResponse } from '../../types';
import type { UserSummary } from '../../types/social';

interface UserSearchPage {
  users: UserSummary[];
  totalElements: number;
  totalPages: number;
}

export function SocialFeedPage() {
  const { user } = useAuthStore();
  const navigate  = useNavigate();
  const [page, setPage]           = useState(0);
  const [searchQuery, setSearchQuery] = useState('');
  const [searchActive, setSearchActive] = useState(false);

  const feedKey  = ['socialFeed', page];
  const trendKey = ['trendingHashtags'];

  const { data: feed, isLoading } = useQuery({
    queryKey: feedKey,
    queryFn: () => socialService.getFeed(page, 20),
  });

  const { data: trending } = useQuery({
    queryKey: trendKey,
    queryFn: () => socialService.getTrendingHashtags(10),
    staleTime: 5 * 60_000,
  });

  // User search — fires when query >= 2 chars
  const { data: searchResults, isLoading: searchLoading } = useQuery({
    queryKey: ['userSearch', searchQuery],
    queryFn: async () => {
      const res = await apiClient.get<ApiResponse<UserSearchPage>>(
        `/users/search?q=${encodeURIComponent(searchQuery)}&size=8`);
      return res.data.data!;
    },
    enabled: searchQuery.length >= 2,
    staleTime: 30_000,
  });

  return (
    <div className="min-h-screen bg-neutral-950 text-white">
      {/* Header */}
      <header className="border-b border-neutral-800 px-6 py-4 flex items-center justify-between
                         sticky top-0 bg-neutral-950 z-10">
        <div className="flex items-center gap-4">
          <button onClick={() => navigate(-1)}
            className="text-neutral-400 hover:text-white transition-colors">
            <ArrowLeft className="w-4 h-4" />
          </button>
          <h1 className="text-xl font-bold">
            Cinema<span className="text-amber-400">Book</span>
            <span className="ml-2 text-sm font-normal text-neutral-400">Social</span>
          </h1>
        </div>
        <Link to="/dashboard"
          className="text-sm text-neutral-400 hover:text-white transition-colors">
          Browse films →
        </Link>
      </header>

      <main className="max-w-5xl mx-auto px-4 py-6">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">

          {/* ── Feed column ─────────────────────────────────────── */}
          <div className="md:col-span-2 space-y-4">
            <CreatePostForm
              queryKeys={[feedKey]}
              placeholder="Share something about cinema… Use #hashtags!" />

            {isLoading ? (
              <div className="space-y-4">
                {Array.from({ length: 5 }).map((_, i) => (
                  <div key={i} className="h-40 bg-neutral-900 border border-neutral-800
                                          rounded-xl animate-pulse" />
                ))}
              </div>
            ) : feed?.posts.length === 0 ? (
              <div className="text-center py-16 bg-neutral-900 border border-neutral-800 rounded-xl">
                <Users className="w-10 h-10 text-neutral-700 mx-auto mb-3" />
                <p className="text-neutral-400 mb-1">Your feed is empty.</p>
                <p className="text-neutral-500 text-sm mb-4">
                  Search for users to follow and see their posts here.
                </p>
                <button onClick={() => setSearchActive(true)}
                  className="text-sm text-amber-400 hover:underline">
                  Find users to follow →
                </button>
              </div>
            ) : (
              <>
                {feed?.posts.map(post => (
                  <PostCard key={post.id} post={post} queryKeys={[feedKey]} />
                ))}
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

          {/* ── Sidebar ──────────────────────────────────────────── */}
          <div className="space-y-4">

            {/* User search */}
            <div className="bg-neutral-900 border border-neutral-800 rounded-xl p-4">
              <h3 className="flex items-center gap-2 font-semibold mb-3 text-sm">
                <Users className="w-4 h-4 text-amber-400" />
                Find users to follow
              </h3>
              <div className="relative mb-3">
                <Search className="w-3.5 h-3.5 absolute left-3 top-1/2 -translate-y-1/2
                                   text-neutral-500" />
                <input
                  value={searchQuery}
                  onChange={e => { setSearchQuery(e.target.value); setSearchActive(true); }}
                  placeholder="Search by name or username…"
                  className="w-full bg-neutral-800 border border-neutral-700 rounded-lg
                             pl-9 pr-3 py-2 text-sm text-white placeholder-neutral-600
                             focus:outline-none focus:border-amber-400 transition-colors" />
              </div>

              {/* Search results */}
              {searchQuery.length >= 2 && (
                <div className="space-y-1.5">
                  {searchLoading ? (
                    <p className="text-xs text-neutral-500 text-center py-2">Searching…</p>
                  ) : searchResults?.users.length === 0 ? (
                    <p className="text-xs text-neutral-500 text-center py-2">
                      No users found for "{searchQuery}"
                    </p>
                  ) : (
                    searchResults?.users.map(u => (
                      <Link key={u.id} to={`/profile/${u.username}`}
                        className="flex items-center gap-2.5 px-3 py-2 rounded-lg
                                   hover:bg-neutral-800 transition-colors group">
                        {/* Mini avatar */}
                        <div className="w-7 h-7 rounded-full bg-neutral-700 flex items-center
                                        justify-center text-xs font-bold flex-shrink-0">
                          {u.avatarUrl
                            ? <img src={u.avatarUrl} alt=""
                                   className="w-full h-full rounded-full object-cover" />
                            : u.displayName?.[0]?.toUpperCase() ?? '?'}
                        </div>
                        <div className="min-w-0">
                          <p className="text-sm font-medium truncate
                                        group-hover:text-amber-400 transition-colors">
                            {u.displayName}
                          </p>
                          <p className="text-xs text-neutral-500">@{u.username}</p>
                        </div>
                        {u.followedByMe && (
                          <span className="ml-auto text-xs text-green-500 flex-shrink-0">
                            Following
                          </span>
                        )}
                      </Link>
                    ))
                  )}
                </div>
              )}

              {searchQuery.length < 2 && (
                <p className="text-xs text-neutral-600 text-center">
                  Type at least 2 characters to search
                </p>
              )}
            </div>

            {/* Trending hashtags */}
            <div className="bg-neutral-900 border border-neutral-800 rounded-xl p-5">
              <h3 className="flex items-center gap-2 font-semibold mb-4 text-sm">
                <TrendingUp className="w-4 h-4 text-amber-400" />
                Trending hashtags
              </h3>
              {!trending || trending.length === 0 ? (
                <p className="text-neutral-500 text-sm">No trending hashtags yet.</p>
              ) : (
                <div className="space-y-2">
                  {trending.map(h => (
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