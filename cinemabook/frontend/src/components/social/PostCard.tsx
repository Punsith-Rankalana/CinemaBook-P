import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Heart, Trash2, Film, Clock } from 'lucide-react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { socialService } from '../../services/socialService';
import { useAuthStore } from '../../store/authStore';
import type { Post } from '../../types/social';

function timeAgo(iso: string): string {
  const diff = Date.now() - new Date(iso).getTime();
  const min  = Math.floor(diff / 60000);
  if (min < 1)  return 'just now';
  if (min < 60) return `${min}m ago`;
  const h = Math.floor(min / 60);
  if (h  < 24)  return `${h}h ago`;
  return `${Math.floor(h / 24)}d ago`;
}

function expiresIn(iso: string): string {
  const diff = new Date(iso).getTime() - Date.now();
  if (diff <= 0) return 'expired';
  const h = Math.floor(diff / 3600000);
  if (h < 24)   return `${h}h left`;
  return `${Math.floor(h / 24)}d left`;
}

interface PostCardProps {
  post: Post;
  queryKeys?: string[][];   // react-query keys to invalidate after like/delete
}

export function PostCard({ post, queryKeys = [] }: PostCardProps) {
  const { user } = useAuthStore();
  const qc = useQueryClient();
  const [liked,      setLiked]      = useState(post.likedByMe);
  const [likeCount,  setLikeCount]  = useState(post.likesCount);
  const isOwn = user?.id === post.author.id;

  const invalidate = () =>
    queryKeys.forEach(k => qc.invalidateQueries({ queryKey: k }));

  const likeMutation = useMutation({
    mutationFn: () => socialService.togglePostLike(post.id),
    onMutate: () => {
      // Optimistic update
      setLiked(l => !l);
      setLikeCount(c => liked ? c - 1 : c + 1);
    },
    onSuccess: (data) => {
      setLiked(data.liked);
      setLikeCount(data.newCount);
    },
    onError: () => {
      // Revert on error
      setLiked(l => !l);
      setLikeCount(c => liked ? c + 1 : c - 1);
    },
  });

  const deleteMutation = useMutation({
    mutationFn: () => socialService.deletePost(post.id),
    onSuccess: invalidate,
  });

  return (
    <div className="bg-neutral-900 border border-neutral-800 rounded-xl p-5
                    hover:border-neutral-700 transition-colors">
      {/* Header */}
      <div className="flex items-start justify-between mb-3">
        <div className="flex items-center gap-3">
          {/* Avatar */}
          <div className="w-9 h-9 rounded-full bg-neutral-700 flex items-center justify-center
                          text-sm font-bold text-white flex-shrink-0">
            {post.author.avatarUrl
              ? <img src={post.author.avatarUrl} alt=""
                     className="w-full h-full rounded-full object-cover" />
              : post.author.displayName?.[0]?.toUpperCase() ?? '?'}
          </div>
          <div>
            <Link to={`/profile/${post.author.username}`}
              className="text-sm font-semibold hover:text-amber-400 transition-colors">
              {post.author.displayName}
            </Link>
            <p className="text-xs text-neutral-500">
              @{post.author.username} · {timeAgo(post.createdAt)}
            </p>
          </div>
        </div>
        {isOwn && (
          <button onClick={() => deleteMutation.mutate()}
            disabled={deleteMutation.isPending}
            className="text-neutral-600 hover:text-red-400 transition-colors p-1">
            <Trash2 className="w-4 h-4" />
          </button>
        )}
      </div>

      {/* Film tag */}
      {post.filmId && post.filmTitle && (
        <Link to={`/films/${post.filmId}`}
          className="inline-flex items-center gap-1.5 text-xs bg-amber-400/10
                     border border-amber-400/30 text-amber-400 px-2.5 py-1
                     rounded-full mb-3 hover:bg-amber-400/20 transition-colors">
          <Film className="w-3 h-3" /> {post.filmTitle}
        </Link>
      )}

      {/* Content */}
      <p className="text-sm text-neutral-200 leading-relaxed whitespace-pre-wrap mb-3">
        {post.content}
      </p>

      {/* Media */}
      {post.mediaUrl && post.postType === 'IMAGE' && (
        <img src={post.mediaUrl} alt="Post media"
          className="w-full rounded-lg object-cover max-h-80 mb-3 border border-neutral-800" />
      )}
      {post.mediaUrl && post.postType === 'CLIP' && (
        <video src={post.mediaUrl} controls
          className="w-full rounded-lg max-h-60 mb-3 border border-neutral-800" />
      )}

      {/* Hashtags */}
      {post.hashtags.length > 0 && (
        <div className="flex flex-wrap gap-1.5 mb-3">
          {post.hashtags.map(tag => (
            <Link key={tag} to={`/social/hashtag/${tag}`}
              className="text-xs text-blue-400 hover:text-blue-300 transition-colors">
              #{tag}
            </Link>
          ))}
        </div>
      )}

      {/* Footer */}
      <div className="flex items-center justify-between pt-3 border-t border-neutral-800">
        <button onClick={() => likeMutation.mutate()}
          disabled={likeMutation.isPending}
          className={`flex items-center gap-1.5 text-sm transition-colors
            ${liked ? 'text-red-400' : 'text-neutral-500 hover:text-red-400'}`}>
          <Heart className={`w-4 h-4 ${liked ? 'fill-current' : ''}`} />
          {likeCount > 0 && <span>{likeCount}</span>}
        </button>

        <div className="flex items-center gap-1 text-xs text-neutral-600">
          <Clock className="w-3 h-3" />
          <span>{expiresIn(post.autoDeleteAt)}</span>
        </div>
      </div>
    </div>
  );
}