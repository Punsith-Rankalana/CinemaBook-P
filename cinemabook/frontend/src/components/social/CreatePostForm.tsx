import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Send, Film } from 'lucide-react';
import { socialService } from '../../services/socialService';

interface CreatePostFormProps {
  filmId?:    string;   // pre-tag a film
  filmTitle?: string;
  queryKeys?: string[][];
  placeholder?: string;
}

export function CreatePostForm({
  filmId, filmTitle, queryKeys = [], placeholder = 'Share something about cinema…'
}: CreatePostFormProps) {
  const qc = useQueryClient();
  const [content, setContent] = useState('');
  const [error,   setError]   = useState('');

  const mutation = useMutation({
    mutationFn: () => socialService.createPost({
      content,
      postType: 'TEXT',
      filmId: filmId ?? undefined,
    }),
    onSuccess: () => {
      setContent('');
      setError('');
      queryKeys.forEach(k => qc.invalidateQueries({ queryKey: k }));
    },
    onError: (err: any) => {
      setError(err?.response?.data?.error ?? 'Failed to post. Please try again.');
    },
  });

  const handleSubmit = () => {
    if (!content.trim()) { setError('Post cannot be empty.'); return; }
    mutation.mutate();
  };

  const remaining = 2000 - content.length;

  return (
    <div className="bg-neutral-900 border border-neutral-800 rounded-xl p-4">
      {filmId && filmTitle && (
        <div className="flex items-center gap-1.5 text-xs text-amber-400 mb-3">
          <Film className="w-3 h-3" /> Tagging: <strong>{filmTitle}</strong>
        </div>
      )}

      <textarea
        value={content}
        onChange={e => setContent(e.target.value)}
        onKeyDown={e => {
          if (e.key === 'Enter' && e.ctrlKey) handleSubmit();
        }}
        placeholder={placeholder}
        rows={3}
        maxLength={2000}
        className="w-full bg-transparent text-sm text-neutral-200 placeholder-neutral-600
                   resize-none focus:outline-none"
      />

      {error && <p className="text-xs text-red-400 mt-1">{error}</p>}

      <div className="flex items-center justify-between mt-3 pt-3 border-t border-neutral-800">
        <span className={`text-xs ${remaining < 100 ? 'text-amber-400' : 'text-neutral-600'}`}>
          {remaining} chars left · Use #hashtags to tag topics
        </span>
        <button onClick={handleSubmit}
          disabled={mutation.isPending || !content.trim()}
          className="flex items-center gap-2 bg-amber-400 hover:bg-amber-300 disabled:opacity-50
                     text-neutral-950 font-semibold px-4 py-1.5 rounded-lg text-sm transition-colors">
          <Send className="w-3.5 h-3.5" />
          {mutation.isPending ? 'Posting…' : 'Post'}
        </button>
      </div>
    </div>
  );
}