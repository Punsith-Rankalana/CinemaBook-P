import { useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { ArrowLeft, UserPlus, UserMinus, Users } from 'lucide-react';
import { socialService } from '../../services/socialService';
import { PostCard } from '../../components/social/PostCard';
import { useAuthStore } from '../../store/authStore';
import { apiClient } from '../../services/apiClient';
import type { ApiResponse } from '../../types';
import type { UserSummary } from '../../types/social';

interface PublicProfile {
  id: string;
  username: string;
  displayName: string;
  avatarUrl?: string;
  bio?: string;
  userType: string;
}

export function UserProfilePage() {
  const { username } = useParams<{ username: string }>();
  const { user: me } = useAuthStore();
  const navigate = useNavigate();
  const qc = useQueryClient();
  const [postsPage, setPostsPage] = useState(0);
  const [activeTab, setActiveTab] = useState<'posts' | 'followers' | 'following'>('posts');

  // Fetch public profile
  const { data: profile, isLoading, isError } = useQuery({
    queryKey: ['publicProfile', username],
    queryFn: async () => {
      const res = await apiClient.get<ApiResponse<PublicProfile>>(`/users/${username}`);
      return res.data.data!;
    },
    enabled: !!username,
  });

  // Followers list (also used to check followedByMe)
  const followersKey = ['profileFollowers', profile?.id];
  const { data: followers } = useQuery({
    queryKey: followersKey,
    queryFn: () => socialService.getFollowers(profile!.id, 0, 100),
    enabled: !!profile?.id,
  });

  // Following list
  const { data: following } = useQuery({
    queryKey: ['profileFollowing', profile?.id],
    queryFn: () => socialService.getFollowing(profile!.id, 0, 100),
    enabled: !!profile?.id && activeTab === 'following',
  });

  // Posts
  const postsKey = ['userProfilePosts', profile?.id, postsPage];
  const { data: posts, isLoading: postsLoading } = useQuery({
    queryKey: postsKey,
    queryFn: () => socialService.getUserPosts(profile!.id, postsPage, 10),
    enabled: !!profile?.id && activeTab === 'posts',
  });

  const followedByMe = me
    ? followers?.users.some(u => u.id === me.id) ?? false
    : false;

  const followMutation = useMutation({
    mutationFn: () => socialService.toggleFollow(profile!.id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: followersKey });
    },
  });

  const isOwnProfile = me?.username === username;

  if (isLoading) {
    return (
      <div className="min-h-screen bg-neutral-950 flex items-center justify-center">
        <div className="w-8 h-8 border-2 border-amber-400 border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  if (isError || !profile) {
    return (
      <div className="min-h-screen bg-neutral-950 text-white flex items-center justify-center">
        <div className="text-center">
          <p className="text-neutral-400 mb-4">User not found.</p>
          <Link to="/social" className="text-amber-400 hover:underline text-sm">← Back to social</Link>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-neutral-950 text-white">
      <header className="border-b border-neutral-800 px-6 py-4 flex items-center gap-4
                         sticky top-0 bg-neutral-950 z-10">
        <button onClick={() => navigate(-1)}
          className="text-neutral-400 hover:text-white transition-colors">
          <ArrowLeft className="w-4 h-4" />
        </button>
        <h1 className="text-xl font-bold">
          Cinema<span className="text-amber-400">Book</span>
        </h1>
      </header>

      <main className="max-w-2xl mx-auto px-4 py-8">

        {/* Profile card */}
        <div className="bg-neutral-900 border border-neutral-800 rounded-2xl p-6 mb-6">
          <div className="flex items-start justify-between mb-4">
            <div className="flex items-center gap-4">
              {/* Avatar */}
              <div className="w-16 h-16 rounded-full bg-neutral-700 flex items-center
                              justify-center text-2xl font-bold flex-shrink-0">
                {profile.avatarUrl
                  ? <img src={profile.avatarUrl} alt=""
                         className="w-full h-full rounded-full object-cover" />
                  : profile.displayName?.[0]?.toUpperCase() ?? '?'}
              </div>
              <div>
                <h2 className="text-xl font-bold">{profile.displayName}</h2>
                <p className="text-neutral-400 text-sm">@{profile.username}</p>
                <span className={`text-xs px-2 py-0.5 rounded-full mt-1 inline-block
                  ${profile.userType === 'FILM_OWNER'
                    ? 'bg-amber-400/10 text-amber-400 border border-amber-400/30'
                    : 'bg-neutral-800 text-neutral-400'}`}>
                  {profile.userType === 'FILM_OWNER' ? '🎬 Film Owner' : '👁 Watcher'}
                </span>
              </div>
            </div>

            {/* Follow / Unfollow button */}
            {!isOwnProfile && me && (
              <button
                onClick={() => followMutation.mutate()}
                disabled={followMutation.isPending}
                className={`flex items-center gap-2 px-4 py-2 rounded-lg text-sm
                            font-semibold transition-colors disabled:opacity-50
                  ${followedByMe
                    ? 'bg-neutral-800 hover:bg-red-900/40 text-neutral-300 hover:text-red-400 border border-neutral-700 hover:border-red-800'
                    : 'bg-amber-400 hover:bg-amber-300 text-neutral-950'}`}>
                {followMutation.isPending
                  ? '…'
                  : followedByMe
                    ? <><UserMinus className="w-4 h-4" /> Unfollow</>
                    : <><UserPlus className="w-4 h-4" /> Follow</>}
              </button>
            )}
          </div>

          {profile.bio && (
            <p className="text-neutral-300 text-sm mb-4">{profile.bio}</p>
          )}

          {/* Stats row */}
          <div className="flex gap-6">
            <button onClick={() => setActiveTab('followers')}
              className={`text-center hover:text-amber-400 transition-colors
                          ${activeTab === 'followers' ? 'text-amber-400' : ''}`}>
              <div className="text-lg font-bold">{followers?.totalElements ?? 0}</div>
              <div className="text-xs text-neutral-500">Followers</div>
            </button>
            <button onClick={() => setActiveTab('following')}
              className={`text-center hover:text-amber-400 transition-colors
                          ${activeTab === 'following' ? 'text-amber-400' : ''}`}>
              <div className="text-lg font-bold">{following?.totalElements ?? 0}</div>
              <div className="text-xs text-neutral-500">Following</div>
            </button>
            <button onClick={() => setActiveTab('posts')}
              className={`text-center hover:text-amber-400 transition-colors
                          ${activeTab === 'posts' ? 'text-amber-400' : ''}`}>
              <div className="text-lg font-bold">{posts?.totalElements ?? '…'}</div>
              <div className="text-xs text-neutral-500">Posts</div>
            </button>
          </div>
        </div>

        {/* Tab selector */}
        <div className="flex gap-1 mb-5 bg-neutral-900 border border-neutral-800
                        rounded-xl p-1 w-fit">
          {(['posts', 'followers', 'following'] as const).map(tab => (
            <button key={tab} onClick={() => setActiveTab(tab)}
              className={`px-4 py-2 rounded-lg text-sm font-medium capitalize transition-colors
                ${activeTab === tab
                  ? 'bg-amber-400 text-neutral-950'
                  : 'text-neutral-400 hover:text-white'}`}>
              {tab}
            </button>
          ))}
        </div>

        {/* Posts tab */}
        {activeTab === 'posts' && (
          <div className="space-y-4">
            {postsLoading
              ? Array.from({ length: 3 }).map((_, i) => (
                  <div key={i} className="h-40 bg-neutral-900 border border-neutral-800
                                          rounded-xl animate-pulse" />
                ))
              : posts?.posts.length === 0
                ? <div className="text-center py-12 text-neutral-500 text-sm">No posts yet.</div>
                : <>
                    {posts?.posts.map(post => (
                      <PostCard key={post.id} post={post} queryKeys={[postsKey]} />
                    ))}
                    {posts && posts.totalPages > 1 && (
                      <div className="flex justify-center gap-3 pt-2">
                        <button onClick={() => setPostsPage(p => Math.max(0, p - 1))}
                          disabled={postsPage === 0}
                          className="px-4 py-2 text-sm bg-neutral-800 rounded-lg
                                     disabled:opacity-40 hover:bg-neutral-700 transition-colors">
                          Previous
                        </button>
                        <span className="text-sm text-neutral-400 self-center">
                          {postsPage + 1} / {posts.totalPages}
                        </span>
                        <button onClick={() => setPostsPage(p => Math.min(posts.totalPages - 1, p + 1))}
                          disabled={postsPage >= posts.totalPages - 1}
                          className="px-4 py-2 text-sm bg-neutral-800 rounded-lg
                                     disabled:opacity-40 hover:bg-neutral-700 transition-colors">
                          Next
                        </button>
                      </div>
                    )}
                  </>}
          </div>
        )}

        {/* Followers tab */}
        {activeTab === 'followers' && (
          <UserList
            users={followers?.users ?? []}
            emptyMsg="No followers yet." />
        )}

        {/* Following tab */}
        {activeTab === 'following' && (
          <UserList
            users={following?.users ?? []}
            emptyMsg="Not following anyone yet." />
        )}
      </main>
    </div>
  );
}

function UserList({ users, emptyMsg }: {
  users: UserSummary[];
  emptyMsg: string;
}) {
  if (users.length === 0) {
    return (
      <div className="text-center py-12 text-neutral-500 text-sm">{emptyMsg}</div>
    );
  }
  return (
    <div className="space-y-2">
      {users.map(u => (
        <Link key={u.id} to={`/profile/${u.username}`}
          className="flex items-center gap-3 bg-neutral-900 border border-neutral-800
                     rounded-xl px-4 py-3 hover:border-neutral-600 transition-colors">
          <div className="w-9 h-9 rounded-full bg-neutral-700 flex items-center
                          justify-center text-sm font-bold flex-shrink-0">
            {u.avatarUrl
              ? <img src={u.avatarUrl} alt=""
                     className="w-full h-full rounded-full object-cover" />
              : u.displayName?.[0]?.toUpperCase() ?? '?'}
          </div>
          <div>
            <p className="text-sm font-medium">{u.displayName}</p>
            <p className="text-xs text-neutral-500">@{u.username}</p>
          </div>
          <div className="ml-auto flex items-center gap-1 text-xs text-neutral-600">
            <Users className="w-3.5 h-3.5" />
            {u.followersCount}
          </div>
        </Link>
      ))}
    </div>
  );
}