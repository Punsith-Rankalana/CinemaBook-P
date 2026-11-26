import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Heart, Trash2, MessageCircle, Send } from 'lucide-react';
import { socialService } from '../../services/socialService';
import { useAuthStore } from '../../store/authStore';
import type { Comment } from '../../types/social';

interface FilmCommentsProps {
  filmId: string;
}

function timeAgo(iso: string): string {
  const diff = Date.now() - new Date(iso).getTime();
  const min  = Math.floor(diff / 60000);
  if (min < 1)  return 'just now';
  if (min < 60) return `${min}m ago`;
  const h = Math.floor(min / 60);
  if (h  < 24)  return `${h}h ago`;
  return `${Math.floor(h / 24)}d ago`;
}

export function FilmComments({ filmId }: FilmCommentsProps) {
  const { user } = useAuthStore();
  const qc       = useQueryClient();
  const [page, setPage]     = useState(0);
  const [content, setContent] = useState('');
  const [error, setError]   = useState('');

  const queryKey = ['filmComments', filmId, page];

  const { data, isLoading } = useQuery({
    queryKey,
    queryFn: () => socialService.getFilmComments(filmId, page, 10),
  });

  const addMutation = useMutation({
    mutationFn: () => socialService.addFilmComment(filmId, { content }),
    onSuccess: () => {
      setContent('');
      setError('');
      qc.invalidateQueries({ queryKey: ['filmComments', filmId] });
    },
    onError: (err: any) => {
      setError(err?.response?.data?.error ?? 'Failed to post comment.');
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => socialService.deleteComment(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['filmComments', filmId] }),
  });

  const likeMutation = useMutation({
    mutationFn: (id: string) => socialService.toggleCommentLike(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['filmComments', filmId] }),
  });

  return (
    <div className="mt-8">
      <h3 className="flex items-center gap-2 font-semibold text-lg mb-4">
        <MessageCircle className="w-5 h-5 text-amber-400" />
        Comments
        {data && <span className="text-neutral-500 text-sm font-normal">({data.totalElements})</span>}
      </h3>

      {/* Write comment */}
      {user && (
        <div className="bg-neutral-900 border border-neutral-800 rounded-xl p-4 mb-6">
          <textarea
            value={content}
            onChange={e => setContent(e.target.value)}
            placeholder="Write a comment about this film…"
            rows={3}
            maxLength={2000}
            className="w-full bg-transparent text-sm text-neutral-200 placeholder-neutral-600
                       resize-none focus:outline-none"
          />
          {error && <p className="text-xs text-red-400 mt-1">{error}</p>}
          <div className="flex justify-between items-center mt-3 pt-3 border-t border-neutral-800">
            <span className="text-xs text-neutral-600">{2000 - content.length} chars left</span>
            <button
              onClick={() => { if (!content.trim()) { setError('Comment cannot be empty.'); return; } addMutation.mutate(); }}
              disabled={addMutation.isPending || !content.trim()}
              className="flex items-center gap-2 bg-amber-400 hover:bg-amber-300 disabled:opacity-50
                         text-neutral-950 font-semibold px-4 py-1.5 rounded-lg text-sm transition-colors">
              <Send className="w-3.5 h-3.5" />
              {addMutation.isPending ? 'Posting…' : 'Comment'}
            </button>
          </div>
        </div>
      )}

      {/* Comments list */}
      {isLoading ? (
        <div className="space-y-3">
          {Array.from({ length: 3 }).map((_, i) => (
            <div key={i} className="h-20 bg-neutral-900 border border-neutral-800 rounded-xl animate-pulse" />
          ))}
        </div>
      ) : data?.comments.length === 0 ? (
        <div className="text-center py-10 text-neutral-500 text-sm">
          No comments yet. Be the first to share your thoughts!
        </div>
      ) : (
        <div className="space-y-3">
          {data?.comments.map(comment => (
            <CommentCard
              key={comment.id}
              comment={comment}
              onLike={() => likeMutation.mutate(comment.id)}
              onDelete={() => deleteMutation.mutate(comment.id)}
              likeLoading={likeMutation.isPending}
              deleteLoading={deleteMutation.isPending}
            />
          ))}
        </div>
      )}

      {/* Pagination */}
      {data && data.totalPages > 1 && (
        <div className="flex justify-center gap-3 mt-5">
          <button onClick={() => setPage(p => Math.max(0, p - 1))}
            disabled={page === 0}
            className="px-4 py-2 text-sm bg-neutral-800 rounded-lg disabled:opacity-40
                       hover:bg-neutral-700 transition-colors">Previous</button>
          <span className="text-sm text-neutral-400 self-center">
            {page + 1} / {data.totalPages}
          </span>
          <button onClick={() => setPage(p => Math.min(data.totalPages - 1, p + 1))}
            disabled={page >= data.totalPages - 1}
            className="px-4 py-2 text-sm bg-neutral-800 rounded-lg disabled:opacity-40
                       hover:bg-neutral-700 transition-colors">Next</button>
        </div>
      )}
    </div>
  );
}

function CommentCard({ comment, onLike, onDelete, likeLoading, deleteLoading }: {
  comment: Comment;
  onLike: () => void;
  onDelete: () => void;
  likeLoading: boolean;
  deleteLoading: boolean;
}) {
  const [liked, setLiked] = useState(comment.likedByMe);
  const [count, setCount] = useState(comment.likesCount);

  return (
    <div className="bg-neutral-900 border border-neutral-800 rounded-xl p-4">
      <div className="flex items-start justify-between mb-2">
        <div className="flex items-center gap-2">
          <div className="w-7 h-7 rounded-full bg-neutral-700 flex items-center justify-center
                          text-xs font-bold flex-shrink-0">
            {comment.author.displayName?.[0]?.toUpperCase() ?? '?'}
          </div>
          <div>
            <span className="text-sm font-medium">{comment.author.displayName}</span>
            <span className="text-xs text-neutral-500 ml-2">@{comment.author.username}</span>
            <span className="text-xs text-neutral-600 ml-2">· {timeAgo(comment.createdAt)}</span>
          </div>
        </div>
        {comment.ownedByMe && (
          <button onClick={onDelete} disabled={deleteLoading}
            className="text-neutral-600 hover:text-red-400 transition-colors p-1">
            <Trash2 className="w-3.5 h-3.5" />
          </button>
        )}
      </div>

      <p className="text-sm text-neutral-300 leading-relaxed ml-9 mb-3">
        {comment.content}
      </p>

      <div className="ml-9">
        <button
          onClick={() => {
            setLiked(l => !l);
            setCount(c => liked ? c - 1 : c + 1);
            onLike();
          }}
          disabled={likeLoading}
          className={`flex items-center gap-1.5 text-xs transition-colors
            ${liked ? 'text-red-400' : 'text-neutral-500 hover:text-red-400'}`}>
          <Heart className={`w-3.5 h-3.5 ${liked ? 'fill-current' : ''}`} />
          {count > 0 && <span>{count}</span>}
        </button>
      </div>
    </div>
  );
}