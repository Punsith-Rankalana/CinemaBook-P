import { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { ArrowLeft, Hash } from 'lucide-react';
import { socialService } from '../../services/socialService';
import { PostCard } from '../../components/social/PostCard';

export function HashtagPage() {
  const { tag }   = useParams<{ tag: string }>();
  const [page, setPage] = useState(0);
  const queryKey  = ['hashtagPosts', tag, page];

  const { data, isLoading } = useQuery({
    queryKey,
    queryFn: () => socialService.getHashtagPosts(tag!, page, 20),
    enabled: !!tag,
  });

  return (
    <div className="min-h-screen bg-neutral-950 text-white">
      <header className="border-b border-neutral-800 px-6 py-4 flex items-center gap-4
                         sticky top-0 bg-neutral-950 z-10">
        <Link to="/social"
          className="text-neutral-400 hover:text-white transition-colors">
          <ArrowLeft className="w-4 h-4" />
        </Link>
        <h1 className="text-xl font-bold flex items-center gap-2">
          <Hash className="w-5 h-5 text-amber-400" />
          {tag}
        </h1>
        {data && (
          <span className="text-sm text-neutral-500">{data.totalElements} posts</span>
        )}
      </header>

      <main className="max-w-2xl mx-auto px-4 py-6 space-y-4">
        {isLoading ? (
          Array.from({ length: 5 }).map((_, i) => (
            <div key={i} className="h-40 bg-neutral-900 border border-neutral-800
                                    rounded-xl animate-pulse" />
          ))
        ) : data?.posts.length === 0 ? (
          <div className="text-center py-16 text-neutral-500">
            No posts with #{tag} yet.
          </div>
        ) : (
          <>
            {data?.posts.map(post => (
              <PostCard key={post.id} post={post} queryKeys={[queryKey]} />
            ))}
            {data && data.totalPages > 1 && (
              <div className="flex justify-center gap-3 pt-2">
                <button onClick={() => setPage(p => Math.max(0, p - 1))}
                  disabled={page === 0}
                  className="px-4 py-2 text-sm bg-neutral-800 rounded-lg
                             disabled:opacity-40 hover:bg-neutral-700 transition-colors">
                  Previous
                </button>
                <span className="text-sm text-neutral-400 self-center">
                  {page + 1} / {data.totalPages}
                </span>
                <button onClick={() => setPage(p => Math.min(data.totalPages - 1, p + 1))}
                  disabled={page >= data.totalPages - 1}
                  className="px-4 py-2 text-sm bg-neutral-800 rounded-lg
                             disabled:opacity-40 hover:bg-neutral-700 transition-colors">
                  Next
                </button>
              </div>
            )}
          </>
        )}
      </main>
    </div>
  );
}