# Migration Notes

- Main project content was copied from `E:\NEU\大二下\web开发技术\Youyu`
- This folder is intended to become the new clean project home for `Youyu`
- Runtime and cache directories were intentionally excluded, including:
  - `.claude/`
  - `backend/data/`
  - `backend/target/`
  - `frontend/node_modules/`
  - `frontend/dist/`
  - `frontend/test-results/`
- Outer-repo reference files were copied into `migration-notes/original-root/`
  - `AGENTS.outer-repo.md`
  - `.gitignore.outer-repo`
  - `最终需求文档.md`
  - `.github/workflows/ci.outer-repo.yml`

Next step:

- initialize Git in this folder or connect it to the new GitHub repository once ready
