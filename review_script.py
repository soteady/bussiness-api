# review_script.py

import os
import github
import google.generativeai as genai
import requests # <-- THÊM DÒNG NÀY

# Phiên bản ĐÚNG của hàm get_pr_diff
def get_pr_diff(repo_name, pr_number, github_token):
    """Lấy nội dung thay đổi (diff) của một Pull Request bằng cách gọi API trực tiếp."""
    diff_url = f"https://api.github.com/repos/{repo_name}/pulls/{pr_number}"
    headers = {
        'Accept': 'application/vnd.github.v3.diff',
        'Authorization': f'token {github_token}'
    }
    try:
        response = requests.get(diff_url, headers=headers)
        response.raise_for_status()
        return response.text
    except requests.exceptions.RequestException as e:
        print(f"Lỗi khi lấy PR diff từ API: {e}")
        print(f"Response từ GitHub: {response.text}")
        return None


def get_gemini_review(code_diff, api_key):
    # ... (phần còn lại của hàm này giữ nguyên)
    try:
        genai.configure(api_key=api_key)
        model = genai.GenerativeModel('gemini-1.5-flash-latest')

        prompt = f"""
        Bạn là một chuyên gia review code giàu kinh nghiệm. Hãy phân tích những thay đổi trong Pull Request dưới đây (định dạng diff).
        Đưa ra những nhận xét ngắn gọn, rõ ràng và mang tính xây dựng. Tập trung vào các vấn đề sau:

        1.  **Lỗi tiềm ẩn (Potential Bugs):** Tìm kiếm các lỗi logic, trường hợp ngoại lệ không được xử lý, hoặc các vấn đề có thể gây ra lỗi khi chạy.
        2.  **Cải thiện hiệu suất (Performance):** Gợi ý cách tối ưu hóa thuật toán hoặc cấu trúc dữ liệu để code chạy nhanh hơn hoặc tốn ít tài nguyên hơn.
        3.  **Khả năng đọc (Readability):** Góp ý về cách đặt tên biến, cấu trúc code, hoặc thêm comment để code dễ hiểu hơn.
        4.  **Bảo mật (Security):** Chỉ ra các lỗ hổng bảo mật tiềm tàng như SQL injection, XSS, hoặc xử lý thông tin nhạy cảm không an toàn.
        5.  **Thực hành tốt nhất (Best Practices):** Đối chiếu với các tiêu chuẩn code của ngôn ngữ lập trình và đưa ra gợi ý để tuân thủ.

        Hãy trình bày kết quả dưới dạng Markdown với các đề mục rõ ràng. Nếu không có góp ý nào đáng kể, hãy trả lời "Tôi đã xem qua và không có góp ý nào đáng kể. Good job!".

        Dưới đây là nội dung diff:
        ```diff
        {code_diff}
        ```
        """

        response = model.generate_content(prompt)
        return response.text
    except Exception as e:
        print(f"Lỗi khi gọi Gemini API: {e}")
        return f"Đã xảy ra lỗi khi review code: {e}"


def post_pr_comment(repo_name, pr_number, comment_body, github_token):
    # ... (hàm này giữ nguyên)
    try:
        g = github.Github(github_token)
        repo = g.get_repo(repo_name)
        pr = repo.get_pull(pr_number)
        pr.create_issue_comment(comment_body)
        print("Đã đăng bình luận review thành công!")
    except Exception as e:
        print(f"Lỗi khi đăng bình luận: {e}")

# ... (phần if __name__ == "__main__": giữ nguyên)
if __name__ == "__main__":
    github_token = os.environ.get("GITHUB_TOKEN")
    gemini_api_key = os.environ.get("GEMINI_API_KEY")
    repo_name = os.environ.get("GITHUB_REPOSITORY")
    pr_number_str = os.environ.get("GITHUB_REF").split('/')[2]
    pr_number = int(pr_number_str)

    if not all([github_token, gemini_api_key, repo_name, pr_number]):
        print("Lỗi: Thiếu một trong các biến môi trường cần thiết.")
        exit(1)

    print(f"Bắt đầu review PR #{pr_number} tại repository {repo_name}...")

    diff = get_pr_diff(repo_name, pr_number, github_token)

    if diff:
        review_comment = get_gemini_review(diff, gemini_api_key)
        final_comment = f"### 🤖 Gemini AI Code Review\n\n---\n\n{review_comment}"
        post_pr_comment(repo_name, pr_number, final_comment, github_token)
    else:
        print("Không thể lấy nội dung diff của PR.")
