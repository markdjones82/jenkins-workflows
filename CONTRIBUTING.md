# Contributing to jenkins-workflows

Thank you for your interest in contributing to jenkins-workflows! We welcome contributions from the community.

## How to Contribute

### 1. Fork and Clone
1. Fork the repository on GitHub
2. Clone your fork locally:
   ```bash
   git clone https://github.com/your-username/jenkins-workflows.git
   cd jenkins-workflows
   ```
3. Add upstream remote:
   ```bash
   git remote add upstream https://github.com/markdjones82/jenkins-workflows.git
   ```

### 2. Create a Feature Branch
```bash
git checkout -b feature/your-feature-name
```

Branch naming conventions:
- `feature/` for new features
- `fix/` for bug fixes
- `docs/` for documentation
- `refactor/` for code refactoring

### 3. Make Your Changes
- Keep commits focused and descriptive
- Write meaningful commit messages
- Update documentation as needed
- Add examples for new features

### 4. Test Your Changes
- For Groovy libraries: Test in a Jenkins environment
- For Terraform modules: Validate syntax and plan in a test environment
- Verify all changes work as documented

### 5. Commit and Push
```bash
git add .
git commit -m "feat: brief description of your change"
git push origin feature/your-feature-name
```

### 6. Open a Pull Request
1. Go to your fork on GitHub
2. Click "New Pull Request"
3. Ensure the PR description clearly explains:
   - What problem does this solve?
   - How does it solve it?
   - Any breaking changes?

4. Submit the PR

### Code Style

**Groovy:**
- Follow [Groovy style guide](http://www.groovy-lang.org/style-guide.html)
- Add inline comments for complex logic
- Include docstrings for public methods

**Terraform:**
- Use `terraform fmt` to format code
- Follow [Terraform style conventions](https://www.terraform.io/language/syntax/style)
- Document variables and outputs

**Documentation:**
- Use clear, descriptive language
- Include examples where helpful
- Update README if adding new features
- Keep code samples up to date

## Reporting Issues

Found a bug or have a feature request? Please open an issue with:
- Clear title describing the problem
- Detailed description  
- Steps to reproduce (for bugs)
- Expected vs. actual behavior
- Jenkins/Terraform/Kubernetes versions if relevant

## Questions?

Feel free to open a discussion or issue with your questions. We're here to help!

## License

By contributing, you agree that your contributions will be licensed under the same license as the project (MIT).
