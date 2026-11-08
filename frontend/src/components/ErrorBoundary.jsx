import { Component } from 'react'
import { ErrorState } from './Feedback'

export default class ErrorBoundary extends Component {
  state = { failed: false }

  static getDerivedStateFromError() {
    return { failed: true }
  }

  componentDidCatch(error, info) {
    console.error('Page crashed', error, info)
  }

  render() {
    if (!this.state.failed) return this.props.children
    return (
      <div className="page-pad">
        <ErrorState
          message="This page hit an unexpected problem."
          onRetry={() => { this.setState({ failed: false }); window.location.reload() }}
        />
      </div>
    )
  }
}
