import { apiClient } from './apiClient';
import type { ApiResponse } from '../types';
import type {
  Post, PostPage, Comment, CommentPage, UserPage,
  Hashtag, LikeResponse, FollowResponse,
  CreatePostRequest, CreateCommentRequest,
} from '../types/social';

export const socialService = {

  // ── Posts ─────────────────────────────────────────────────────────────────

  async createPost(data: CreatePostRequest): Promise<Post> {
    const res = await apiClient.post<ApiResponse<Post>>('/social/posts', data);
    return res.data.data!;
  },

  async deletePost(postId: string): Promise<void> {
    await apiClient.delete(`/social/posts/${postId}`);
  },

  async getFeed(page = 0, size = 20): Promise<PostPage> {
    const res = await apiClient.get<ApiResponse<PostPage>>(
      `/social/feed?page=${page}&size=${size}`);
    return res.data.data!;
  },

  async getUserPosts(userId: string, page = 0, size = 20): Promise<PostPage> {
    const res = await apiClient.get<ApiResponse<PostPage>>(
      `/social/users/${userId}/posts?page=${page}&size=${size}`);
    return res.data.data!;
  },

  async getFilmPosts(filmId: string, page = 0, size = 20): Promise<PostPage> {
    const res = await apiClient.get<ApiResponse<PostPage>>(
      `/social/films/${filmId}/posts?page=${page}&size=${size}`);
    return res.data.data!;
  },

  async getHashtagPosts(tag: string, page = 0, size = 20): Promise<PostPage> {
    const res = await apiClient.get<ApiResponse<PostPage>>(
      `/social/hashtags/${encodeURIComponent(tag)}/posts?page=${page}&size=${size}`);
    return res.data.data!;
  },

  // ── Likes ─────────────────────────────────────────────────────────────────

  async togglePostLike(postId: string): Promise<LikeResponse> {
    const res = await apiClient.post<ApiResponse<LikeResponse>>(
      `/social/posts/${postId}/like`);
    return res.data.data!;
  },

  async toggleCommentLike(commentId: string): Promise<LikeResponse> {
    const res = await apiClient.post<ApiResponse<LikeResponse>>(
      `/social/comments/${commentId}/like`);
    return res.data.data!;
  },

  // ── Follows ───────────────────────────────────────────────────────────────

  async toggleFollow(userId: string): Promise<FollowResponse> {
    const res = await apiClient.post<ApiResponse<FollowResponse>>(
      `/social/users/${userId}/follow`);
    return res.data.data!;
  },

  async getFollowers(userId: string, page = 0, size = 20): Promise<UserPage> {
    const res = await apiClient.get<ApiResponse<UserPage>>(
      `/social/users/${userId}/followers?page=${page}&size=${size}`);
    return res.data.data!;
  },

  async getFollowing(userId: string, page = 0, size = 20): Promise<UserPage> {
    const res = await apiClient.get<ApiResponse<UserPage>>(
      `/social/users/${userId}/following?page=${page}&size=${size}`);
    return res.data.data!;
  },

  // ── Hashtags ──────────────────────────────────────────────────────────────

  async getTrendingHashtags(limit = 10): Promise<Hashtag[]> {
    const res = await apiClient.get<ApiResponse<Hashtag[]>>(
      `/social/hashtags/trending?limit=${limit}`);
    return res.data.data!;
  },

  async searchHashtags(q: string, limit = 10): Promise<Hashtag[]> {
    const res = await apiClient.get<ApiResponse<Hashtag[]>>(
      `/social/hashtags/search?q=${encodeURIComponent(q)}&limit=${limit}`);
    return res.data.data!;
  },

  // ── Film comments ─────────────────────────────────────────────────────────

  async getFilmComments(filmId: string, page = 0, size = 20): Promise<CommentPage> {
    const res = await apiClient.get<ApiResponse<CommentPage>>(
      `/social/films/${filmId}/comments?page=${page}&size=${size}`);
    return res.data.data!;
  },

  async addFilmComment(filmId: string, data: CreateCommentRequest): Promise<Comment> {
    const res = await apiClient.post<ApiResponse<Comment>>(
      `/social/films/${filmId}/comments`, data);
    return res.data.data!;
  },

  async deleteComment(commentId: string): Promise<void> {
    await apiClient.delete(`/social/comments/${commentId}`);
  },
};