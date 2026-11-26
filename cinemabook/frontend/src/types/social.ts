// ============================================================
// CinemaBook — Social platform types (Phase 4)
// ============================================================

export interface Author {
  id: string;
  username: string;
  displayName: string;
  avatarUrl?: string;
}

export type PostType = 'TEXT' | 'IMAGE' | 'CLIP' | 'AVATAR_CARD';

export interface Post {
  id: string;
  author: Author;
  postType: PostType;
  content: string;
  mediaUrl?: string;
  filmId?: string;
  filmTitle?: string;
  likesCount: number;
  commentsCount: number;
  hashtags: string[];
  likedByMe: boolean;
  createdAt: string;
  autoDeleteAt: string;
}

export interface PostPage {
  posts: Post[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface Comment {
  id: string;
  author: Author;
  content: string;
  likesCount: number;
  likedByMe: boolean;
  ownedByMe: boolean;
  createdAt: string;
}

export interface CommentPage {
  comments: Comment[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface UserSummary {
  id: string;
  username: string;
  displayName: string;
  avatarUrl?: string;
  followersCount: number;
  followingCount: number;
  followedByMe: boolean;
}

export interface UserPage {
  users: UserSummary[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface Hashtag {
  id: string;
  tag: string;
  postCount: number;
}

export interface LikeResponse {
  liked: boolean;
  newCount: number;
}

export interface FollowResponse {
  following: boolean;
  followersCount: number;
}

// ── Requests ──────────────────────────────────────────────────

export interface CreatePostRequest {
  content: string;
  postType?: PostType;
  mediaUrl?: string;
  filmId?: string;
}

export interface CreateCommentRequest {
  content: string;
}